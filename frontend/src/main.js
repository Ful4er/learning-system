import { createApp } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';
import App from './App.vue';
import routes from './routes';
import './styles.css';
import axios from 'axios';

const token = localStorage.getItem('token');
if (token) {
    axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
}

axios.interceptors.response.use(
    response => response,
    error => {
        if (error.response?.status === 401) {
            localStorage.removeItem('token');
            localStorage.removeItem('userId');
            delete axios.defaults.headers.common['Authorization'];
            window.location.href = '/auth';
        }
        return Promise.reject(error);
    }
);

const router = createRouter({
    history: createWebHistory(),
    routes
});

router.onError((error) => {
    console.log('Router error:', error);
});

createApp(App).use(router).mount('#app');