import { createApp } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';
import App from './App.vue';
import routes from './routes';
import './styles.css';
import axios from 'axios';
import api from './api';

const token = localStorage.getItem('token');
if (token) {
    axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
}

const apiBase = import.meta.env.VITE_API_BASE || import.meta.env.VITE_API_BASE_URL || import.meta.env.BASE_URL || '';
axios.defaults.baseURL = apiBase;

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL || '/'),
    routes
});

let isRefreshing = false;
let failedQueue = [];

function processQueue(error, token = null) {
    failedQueue.forEach(p => {
        if (error) p.reject(error);
        else p.resolve(token);
    });
    failedQueue = [];
}

axios.interceptors.response.use(
    response => response,
    async error => {
        const originalRequest = error.config;

        if (error.response?.status === 401 &&
            !originalRequest?._retry &&
            !originalRequest.url?.includes('/auth/refresh')) {

            if (isRefreshing) {
                return new Promise((resolve, reject) => {
                    failedQueue.push({ resolve, reject });
                }).then(token => {
                    originalRequest.headers['Authorization'] = `Bearer ${token}`;
                    return axios(originalRequest);
                }).catch(err => Promise.reject(err));
            }

            originalRequest._retry = true;
            isRefreshing = true;

            try {
                const resp = await axios.post('/api/auth/refresh');
                const newToken = resp.data?.token;
                if (newToken) {
                    localStorage.setItem('token', newToken);
                    axios.defaults.headers.common['Authorization'] = `Bearer ${newToken}`;
                    processQueue(null, newToken);
                    originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
                    return axios(originalRequest);
                }
            } catch (err) {
                processQueue(err, null);
                localStorage.removeItem('token');
                localStorage.removeItem('userId');
                localStorage.removeItem('userRole');
                delete axios.defaults.headers.common['Authorization'];

                if (router.currentRoute.value.path !== '/auth') {
                    router.push('/auth');
                }
            } finally {
                isRefreshing = false;
            }

            return Promise.reject(error);
        }

        return Promise.reject(error);
    }
);

router.beforeEach(async (to, from, next) => {
    const token = localStorage.getItem('token');

    if (to.path === '/auth') {
        if (token) {
            const role = localStorage.getItem('userRole');
            if (role) {
                const home = role === 'TEACHER' ? '/teacher/profile' :
                    (role === 'STUDENT' ? '/student/profile' : '/');
                return next({ path: home });
            }
        }
        return next();
    }

    if (to.meta?.requiresAuth && !token) {
        return next({ path: '/auth', query: { redirect: to.fullPath } });
    }

    // Role-based route guarding: make sure only teacher can access /teacher/* and student - /student/*
    const needsTeacher = to.path.startsWith('/teacher');
    const needsStudent = to.path.startsWith('/student');

    // If a role is required and we have a token but no cached role, try to fetch it
    if (token && (needsTeacher || needsStudent)) {
        let role = localStorage.getItem('userRole');
        if (!role) {
            try {
                const resp = await api.users.me();
                if (resp.data?.role) {
                    role = resp.data.role;
                    localStorage.setItem('userRole', role);
                }
            } catch (e) {
                // If we cannot determine role, clear session and force auth
                console.warn('Could not determine user role during navigation, redirecting to auth.', e?.message || e);
                localStorage.removeItem('token');
                localStorage.removeItem('userId');
                localStorage.removeItem('userRole');
                delete axios.defaults.headers.common['Authorization'];
                return next({ path: '/auth', query: { redirect: to.fullPath } });
            }
        }

        // Now enforce role restrictions
        const currentRole = localStorage.getItem('userRole');
        if (needsTeacher && currentRole !== 'TEACHER') {
            const redirect = currentRole === 'STUDENT' ? '/student/profile' : '/auth';
            return next({ path: redirect });
        }
        if (needsStudent && currentRole !== 'STUDENT') {
            const redirect = currentRole === 'TEACHER' ? '/teacher/profile' : '/auth';
            return next({ path: redirect });
        }
    }

    next();
});

router.afterEach(async (to, from) => {
    const token = localStorage.getItem('token');
    const role = localStorage.getItem('userRole');

    if (token && !role && to.path !== '/auth') {
        try {
            const resp = await api.users.me();
            if (resp.data?.role) {
                localStorage.setItem('userRole', resp.data.role);
            }
        } catch (e) {
            console.warn('Could not fetch user role in background:', e?.message || e);
        }
    }
});

const app = createApp(App).use(router);

async function initAuth() {
    const token = localStorage.getItem('token');
    if (!token) return;

    try {
        const resp = await api.users.me();
        if (resp.data?.role) {
            localStorage.setItem('userRole', resp.data.role);
        }
    } catch (e) {
        console.warn('Initial auth check failed, clearing session:', e?.message || e);
        localStorage.removeItem('token');
        localStorage.removeItem('userId');
        localStorage.removeItem('userRole');
        delete axios.defaults.headers.common['Authorization'];

        if (window.location.pathname !== '/auth') {
            try {
                await router.push('/auth');
            } catch (err) {
                window.location.href = '/auth';
            }
        }
    }
}

router.onError((error) => {
    console.error('Router error:', error);
});

async function initializeApp() {
    try {
        await initAuth();
    } catch (error) {
        console.error('App initialization failed:', error);
    } finally {
        app.mount('#app');
    }
}

initializeApp();