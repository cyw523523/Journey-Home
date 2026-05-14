import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '../stores/auth'

const routes = [
  { path: '/', name: 'home', component: () => import('../views/HomeView.vue') },
  { path: '/auth', name: 'auth', component: () => import('../views/AuthView.vue') },
  { path: '/community', name: 'community', component: () => import('../views/CommunityView.vue') },
  { path: '/community/:id', name: 'community-detail', component: () => import('../views/CommunityDetailView.vue') },
  { path: '/notices', name: 'notices', component: () => import('../views/NoticeListView.vue') },
  { path: '/notices/:id', name: 'notice-detail', component: () => import('../views/NoticeDetailView.vue') },
  { path: '/animals', name: 'animals', component: () => import('../views/AnimalsView.vue') },
  { path: '/animals/:id', name: 'animal-detail', component: () => import('../views/AnimalDetailView.vue') },
  { path: '/rescues', name: 'rescues', component: () => import('../views/RescuesView.vue') },
  { path: '/users/:id', name: 'user-profile', component: () => import('../views/UserProfileView.vue') },
  { path: '/messages', name: 'messages', component: () => import('../views/MessagesView.vue'), meta: { requiresAuth: true } },
  { path: '/adoptions/new/:animalId', name: 'adoption-new', component: () => import('../views/AdoptionApplyView.vue'), meta: { requiresAuth: true } },
  { path: '/profile', name: 'profile', component: () => import('../views/ProfileView.vue'), meta: { requiresAuth: true } },
  { path: '/admin', name: 'admin', component: () => import('../views/AdminView.vue'), meta: { requiresAuth: true, requiresAdmin: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0, behavior: 'smooth' }
  }
})

router.beforeEach(async (to) => {
  const auth = useAuth()
  if (!auth.state.ready) {
    try {
      await auth.fetchMe()
    } catch {
      auth.logout()
    }
  }
  if (to.meta.requiresAuth && !auth.isLoggedIn.value) {
    return { name: 'auth', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !auth.isAdmin.value) {
    return { name: 'home' }
  }
  return true
})

export default router
