<template>
  <section ref="pageRef" class="view page">
    <div class="hero-board">
      <div
        class="hero-main"
        :style="heroGlowStyle"
        @mousemove="handleHeroMove"
        @mouseleave="resetHeroGlow"
      >
        <div class="hero-glow"></div>
        <div class="hero-particles" aria-hidden="true">
          <span
            v-for="particle in heroParticles"
            :key="particle.id"
            class="hero-particle"
            :style="particle.style"
          />
        </div>

        <div class="hero-showcase" aria-hidden="true">
          <div class="hero-showcase-column hero-showcase-column-a">
            <div class="hero-showcase-track">
              <article v-for="(item, index) in heroShowcaseLoopA" :key="`a-${index}`" class="hero-showcase-card">
                <img :src="item.image" :alt="item.title" />
                <div class="hero-showcase-copy">
                  <strong>{{ item.title }}</strong>
                  <span>{{ item.tagline }}</span>
                </div>
              </article>
            </div>
          </div>
          <div class="hero-showcase-column hero-showcase-column-b">
            <div class="hero-showcase-track">
              <article v-for="(item, index) in heroShowcaseLoopB" :key="`b-${index}`" class="hero-showcase-card">
                <img :src="item.image" :alt="item.title" />
                <div class="hero-showcase-copy">
                  <strong>{{ item.title }}</strong>
                  <span>{{ item.tagline }}</span>
                </div>
              </article>
            </div>
          </div>
        </div>

        <div class="hero-copy">
          <p class="eyebrow"><Sparkles :size="16" /> {{ copy.heroEyebrow }}</p>
          <h1>{{ copy.heroTitle }}</h1>
          <p>{{ copy.heroDescription }}</p>

          <div class="hero-dynamic-line">
            <span>{{ copy.dynamicPrefix }}</span>
            <strong>{{ activeWord }}</strong>
          </div>

          <div class="hero-marquee">
            <span>{{ copy.marquee[0] }}</span>
            <span>{{ copy.marquee[1] }}</span>
            <span>{{ copy.marquee[2] }}</span>
          </div>

          <div class="hero-search">
            <el-input v-model="keyword" size="large" :placeholder="$t('home.searchPlaceholder')" @keyup.enter="goSearch" />
            <el-button :icon="Search" size="large" type="primary" @click="goSearch">{{ $t('common.search') }}</el-button>
          </div>

          <div class="hero-actions">
            <el-button size="large" type="primary" @click="$router.push('/animals')">{{ copy.primaryAction }}</el-button>
            <el-button size="large" plain @click="$router.push('/community')">{{ copy.secondaryAction }}</el-button>
            <el-button size="large" text @click="$router.push('/rescues')">{{ copy.tertiaryAction }}</el-button>
          </div>
        </div>

        <div class="stat-row">
          <div class="stat-item">
            <strong>{{ overview.userCount }}</strong>
            <span>{{ $t('home.registeredUsers') }}</span>
          </div>
          <div class="stat-item">
            <strong>{{ overview.animalCount }}</strong>
            <span>{{ $t('home.animalRecords') }}</span>
          </div>
          <div class="stat-item">
            <strong>{{ overview.rescueCount }}</strong>
            <span>{{ $t('home.rescueInfo') }}</span>
          </div>
          <div class="stat-item">
            <strong>{{ overview.pendingAuditCount }}</strong>
            <span>{{ $t('home.pendingAudit') }}</span>
          </div>
        </div>
      </div>

      <aside class="hero-side">
        <div class="spotlight-photo spotlight-photo-curated">
          <img :src="spotlightImage" alt="pet spotlight" />
          <div class="spotlight-wash"></div>
          <div class="spotlight-badge">
            <Heart :size="18" />
            <span>{{ copy.spotlightText }}</span>
          </div>
        </div>

        <div class="pet-marquee-shell">
          <div class="pet-marquee-head">
            <strong>{{ copy.galleryTitle }}</strong>
          </div>
          <div
            ref="petMarqueeRef"
            class="pet-marquee"
            @pointerdown="startPetMarqueeDrag"
            @pointermove="movePetMarqueeDrag"
            @pointerup="stopPetMarqueeDrag"
            @pointercancel="stopPetMarqueeDrag"
            @pointerleave="stopPetMarqueeDrag"
          >
            <div class="pet-marquee-track">
              <article
                v-for="(pet, index) in marqueePetsLoop"
                :key="`${pet.name}-${index}`"
                class="pet-marquee-card"
              >
                <img :src="pet.image" :alt="pet.name" />
                <div class="pet-marquee-copy">
                  <strong>{{ pet.name }}</strong>
                  <span>{{ pet.tagline }}</span>
                </div>
              </article>
            </div>
          </div>
        </div>

        <div class="side-list">
          <div class="side-list-head">
            <div>
              <h3>{{ copy.noticeTitle }}</h3>
              <p>{{ copy.noticeDescription }}</p>
            </div>
            <el-button text @click="$router.push('/notices')">{{ $t('common.viewAll') }}</el-button>
          </div>

          <RouterLink v-for="notice in notices" :key="notice.id" class="notice-link" :to="`/notices/${notice.id}`">
            <span>{{ notice.title }}</span>
            <ChevronRight :size="16" />
          </RouterLink>

          <EmptyState
            v-if="!notices.length"
            :title="$t('notices.noData')"
            :description="$t('notices.noDataDesc')"
          />
        </div>
      </aside>
    </div>

    <section class="story-stage">
      <div class="story-intro reveal-on-scroll">
        <p class="eyebrow"><Sparkles :size="16" /> {{ copy.storyEyebrow }}</p>
        <h2>{{ copy.storyTitle }}</h2>
        <p>{{ copy.storyDescription }}</p>
      </div>

      <div class="story-grid">
        <article
          v-for="(item, index) in copy.storyCards"
          :key="item.title"
          class="story-card surface reveal-on-scroll"
          :style="{ '--story-accent': item.accent }"
        >
          <span class="story-index">0{{ index + 1 }}</span>
          <h3>{{ item.title }}</h3>
          <p>{{ item.description }}</p>
        </article>
      </div>

      <div class="story-ribbon reveal-on-scroll">
        <div>
          <p>{{ copy.ribbonLabel }}</p>
          <strong>{{ copy.ribbonTitle }}</strong>
        </div>
        <span>{{ copy.ribbonDescription }}</span>
      </div>
    </section>

    <div class="section-head">
      <div>
        <h2>{{ copy.latestAnimalsTitle || $t('home.latestAnimals') }}</h2>
        <p>{{ copy.latestAnimalsDescription || $t('home.latestAnimalsDesc') }}</p>
      </div>
      <el-button :icon="ArrowRight" @click="$router.push('/animals')">{{ $t('common.viewAll') }}</el-button>
    </div>

    <div v-if="animals.length" class="grid animal-grid">
      <AnimalCard v-for="animal in animals" :key="animal.id" :animal="animal" />
    </div>
    <EmptyState v-else :title="$t('home.noAnimals')" :description="$t('home.noAnimalsDesc')" />

    <div class="section-head">
      <div>
        <h2>{{ $t('home.rescueDynamics') }}</h2>
        <p>{{ $t('home.rescueDynamicsDesc') }}</p>
      </div>
      <el-button :icon="Plus" type="primary" @click="$router.push('/rescues')">{{ $t('home.publishRescue') }}</el-button>
    </div>

    <div class="grid rescue-grid">
      <article
        v-for="rescue in rescues"
        :key="rescue.id"
        class="rescue-card lift-card"
        role="button"
        tabindex="0"
        @click="openRescueDetail(rescue.id)"
        @keydown.enter.prevent="openRescueDetail(rescue.id)"
        @keydown.space.prevent="openRescueDetail(rescue.id)"
      >
        <StatusTag :value="rescue.status" :text="rescue.statusText" :options="rescueStatusOptions" />
        <h3>{{ rescue.location }}</h3>
        <p>{{ rescue.animalCondition }}</p>
        <div class="meta-line">
          <Phone :size="16" />
          <span>{{ rescue.contact }}</span>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ArrowRight, ChevronRight, Heart, Phone, Plus, Search, Sparkles } from 'lucide-vue-next'
import AnimalCard from '../components/AnimalCard.vue'
import EmptyState from '../components/EmptyState.vue'
import StatusTag from '../components/StatusTag.vue'
import { homeApi } from '../api'
import { demoAnimals, demoOverview, demoRescues } from '../data/demoData'
import { rescueStatusOptions } from '../utils/status'

const router = useRouter()
const { locale } = useI18n()
const pageRef = ref()
const petMarqueeRef = ref()
const keyword = ref('')
const overview = ref(demoOverview)
const animals = ref(demoAnimals)
const rescues = ref(demoRescues)
const notices = ref([])
const heroParticles = ref([])
const heroGlow = ref({ x: '30%', y: '34%' })
const activeWordIndex = ref(0)
let wordTimer = null
let revealObserver = null
let particleId = 0
let lastParticleAt = 0
let marqueeFrame = null
let isPetMarqueeDragging = false
let petMarqueeStartX = 0
let petMarqueeStartScroll = 0
let petMarqueePauseUntil = 0
const particleTimers = new Set()

const spotlightImage = 'https://images.unsplash.com/photo-1511044568932-338cba0ad803?auto=format&fit=crop&w=1200&q=80'

const heroGallery = {
  zh: [
    {
      title: '软乎乎的小猫',
      tagline: '晒着太阳，安安静静打个盹',
      image: 'https://images.unsplash.com/photo-1511044568932-338cba0ad803?auto=format&fit=crop&w=900&q=80'
    },
    {
      title: '圆滚滚的小狗',
      tagline: '看见人就忍不住摇尾巴',
      image: 'https://images.unsplash.com/photo-1517849845537-4d257902454a?auto=format&fit=crop&w=900&q=80'
    },
    {
      title: '橘白小团子',
      tagline: '抱起来像一团暖呼呼的云',
      image: 'https://images.unsplash.com/photo-1574158622682-e40e69881006?auto=format&fit=crop&w=900&q=80'
    },
    {
      title: '耳朵竖起来的小狗',
      tagline: '总是一脸认真地看着你',
      image: 'https://images.unsplash.com/photo-1548199973-03cce0bbc87b?auto=format&fit=crop&w=900&q=80'
    },
    {
      title: '会眯眼的小黑猫',
      tagline: '熟悉之后其实非常黏人',
      image: 'https://images.unsplash.com/photo-1519052537078-e6302a4968d4?auto=format&fit=crop&w=900&q=80'
    },
    {
      title: '趴着发呆的小奶狗',
      tagline: '看起来像在等一句轻声招呼',
      image: 'https://images.unsplash.com/photo-1518717758536-85ae29035b6d?auto=format&fit=crop&w=900&q=80'
    }
  ],
  en: [
    {
      title: 'Soft little kitten',
      tagline: 'A quiet nap in the sun',
      image: 'https://images.unsplash.com/photo-1511044568932-338cba0ad803?auto=format&fit=crop&w=900&q=80'
    },
    {
      title: 'Round little pup',
      tagline: 'Starts wagging the moment you look over',
      image: 'https://images.unsplash.com/photo-1517849845537-4d257902454a?auto=format&fit=crop&w=900&q=80'
    },
    {
      title: 'Orange fluffball',
      tagline: 'Feels like holding a warm cloud',
      image: 'https://images.unsplash.com/photo-1574158622682-e40e69881006?auto=format&fit=crop&w=900&q=80'
    },
    {
      title: 'Bright-eyed pup',
      tagline: 'Always watching with a serious face',
      image: 'https://images.unsplash.com/photo-1548199973-03cce0bbc87b?auto=format&fit=crop&w=900&q=80'
    },
    {
      title: 'Sleepy black cat',
      tagline: 'Cautious first, clingy later',
      image: 'https://images.unsplash.com/photo-1519052537078-e6302a4968d4?auto=format&fit=crop&w=900&q=80'
    },
    {
      title: 'Tiny resting puppy',
      tagline: 'Looks like it is waiting for a gentle hello',
      image: 'https://images.unsplash.com/photo-1518717758536-85ae29035b6d?auto=format&fit=crop&w=900&q=80'
    }
  ]
}

const petGallery = {
  zh: [
    {
      name: '猫猫日常',
      tagline: '有时候只是看看，也会被治愈',
      image: 'https://images.unsplash.com/photo-1511044568932-338cba0ad803?auto=format&fit=crop&w=900&q=80'
    },
    {
      name: '狗狗表情包',
      tagline: '每一张都像在认真和你打招呼',
      image: 'https://images.unsplash.com/photo-1548199973-03cce0bbc87b?auto=format&fit=crop&w=900&q=80'
    },
    {
      name: '午后小橘猫',
      tagline: '团成一团的时候特别可爱',
      image: 'https://images.unsplash.com/photo-1574158622682-e40e69881006?auto=format&fit=crop&w=900&q=80'
    },
    {
      name: '小奶狗瞬间',
      tagline: '安静看着镜头的时候最戳人',
      image: 'https://images.unsplash.com/photo-1518717758536-85ae29035b6d?auto=format&fit=crop&w=900&q=80'
    },
    {
      name: '黑猫视角',
      tagline: '警惕又好奇，越看越耐看',
      image: 'https://images.unsplash.com/photo-1519052537078-e6302a4968d4?auto=format&fit=crop&w=900&q=80'
    }
  ],
  en: [
    {
      name: 'Cat moments',
      tagline: 'Sometimes a quick look is enough to soften the day',
      image: 'https://images.unsplash.com/photo-1511044568932-338cba0ad803?auto=format&fit=crop&w=900&q=80'
    },
    {
      name: 'Dog faces',
      tagline: 'Every photo feels like a tiny hello',
      image: 'https://images.unsplash.com/photo-1548199973-03cce0bbc87b?auto=format&fit=crop&w=900&q=80'
    },
    {
      name: 'Afternoon kitten',
      tagline: 'Especially cute when curled into a ball',
      image: 'https://images.unsplash.com/photo-1574158622682-e40e69881006?auto=format&fit=crop&w=900&q=80'
    },
    {
      name: 'Puppy pause',
      tagline: 'The calm ones usually hit the hardest',
      image: 'https://images.unsplash.com/photo-1518717758536-85ae29035b6d?auto=format&fit=crop&w=900&q=80'
    },
    {
      name: 'Black cat look',
      tagline: 'Curious, careful, and impossible to ignore',
      image: 'https://images.unsplash.com/photo-1519052537078-e6302a4968d4?auto=format&fit=crop&w=900&q=80'
    }
  ]
}

const zhCopy = {
  heroEyebrow: '从看见开始参与',
  heroTitle: '看见它，也帮它找到回家的路',
  heroDescription: '这里汇集了待领养动物、救助信息和真实照护经验。无论你想带它回家、帮它被更多人看见，还是先了解如何开始，都能找到适合自己的方式。',
  dynamicPrefix: '一起让流浪动物',
  dynamicWords: ['被更多人看见', '更快得到帮助', '慢慢走向新家'],
  marquee: ['领养档案', '救助发布', '社区交流'],
  primaryAction: '看看待领养动物',
  secondaryAction: '进入社区交流',
  tertiaryAction: '发布救助信息',
  spotlightText: '愿每一只等待中的小动物，都能遇到愿意停下来看看它的人。',
  galleryTitle: '可爱相册',
  noticeTitle: '最新公告',
  noticeDescription: '活动通知、平台提醒和重要说明都会更新在这里。',
  storyEyebrow: '你可以这样参与',
  storyTitle: '看见它，也帮它找到回家的路',
  storyDescription: '这里汇集了待领养动物、救助信息和真实照护经验。无论你想带它回家、帮它被更多人看见，还是先了解如何开始，都能找到适合自己的方式。',
  storyCards: [
    {
      title: '想领养它的人',
      description: '先了解动物档案、性格和照护需求，再提交领养申请，认真判断彼此是否合适。',
      accent: 'rgba(36, 92, 80, 0.18)'
    },
    {
      title: '发现需要帮助的人',
      description: '发现受伤、走失或需要安置的动物时，可以快速发布信息，让附近的人及时看到。',
      accent: 'rgba(218, 91, 74, 0.16)'
    },
    {
      title: '愿意分享经验的人',
      description: '把喂养、救助和领养后的照护经验分享出来，让后来的人少走一些弯路。',
      accent: 'rgba(217, 168, 95, 0.22)'
    }
  ],
  ribbonLabel: '一起让善意流动起来',
  ribbonTitle: '看见它、帮助它、陪它找到新的归属',
  ribbonDescription: '从一条信息开始，让需要帮助的动物被更多人看见。',
  latestAnimalsTitle: '最新待领养动物',
  latestAnimalsDescription: '以下信息已通过审核，方便你了解它们的情况与领养状态。'
}

const enCopy = {
  heroEyebrow: 'Give stray animals a better chance to be seen',
  heroTitle: 'One encounter can become the beginning of a real home',
  heroDescription: 'Browse adoptable animals, publish rescue requests, read community stories, and take part in meaningful help through a calmer, friendlier experience.',
  dynamicPrefix: 'Together, we help them',
  dynamicWords: ['get noticed sooner', 'receive care earlier', 'find a safer home'],
  marquee: ['Adoption profiles', 'Rescue posts', 'Community stories'],
  primaryAction: 'Browse adoptable pets',
  secondaryAction: 'Join the community',
  tertiaryAction: 'Publish rescue info',
  spotlightText: 'Every waiting animal deserves someone willing to pause, notice, and care.',
  galleryTitle: 'Pet gallery',
  noticeTitle: 'Latest notices',
  noticeDescription: 'Platform reminders, events, and important updates are collected here.',
  storyEyebrow: 'Ways to take part',
  storyTitle: 'More than a homepage, this should feel like a place worth staying in',
  storyDescription: 'As you scroll, the homepage keeps explaining how the platform supports adopters, rescuers, and people who simply want to contribute.',
  storyCards: [
    {
      title: 'For adopters',
      description: 'Review public profiles, submit applications, and use AI guidance to think more carefully before bringing a pet home.',
      accent: 'rgba(36, 92, 80, 0.18)'
    },
    {
      title: 'For rescuers',
      description: 'If you spot an animal in need, you can quickly publish rescue information so more people can respond in time.',
      accent: 'rgba(218, 91, 74, 0.16)'
    },
    {
      title: 'For the community',
      description: 'Share feeding tips, rescue stories, and post-adoption experience to make the next person feel more prepared.',
      accent: 'rgba(217, 168, 95, 0.22)'
    }
  ],
  ribbonLabel: 'Let kindness keep moving',
  ribbonTitle: 'See them, help them, and walk with them toward a new home',
  ribbonDescription: 'The homepage now behaves more like a polished brand entrance, giving people a reason to stay curious as they scroll.'
}

const copy = computed(() => (locale.value === 'zh' ? zhCopy : enCopy))
const heroShowcaseItems = computed(() => (locale.value === 'zh' ? heroGallery.zh : heroGallery.en))
const heroShowcaseLoopA = computed(() => [...heroShowcaseItems.value.slice(0, 3), ...heroShowcaseItems.value.slice(0, 3)])
const heroShowcaseLoopB = computed(() => [...heroShowcaseItems.value.slice(3), ...heroShowcaseItems.value.slice(3)])
const marqueePets = computed(() => (locale.value === 'zh' ? petGallery.zh : petGallery.en))
const marqueePetsLoop = computed(() => [...marqueePets.value, ...marqueePets.value])
const activeWord = computed(() => copy.value.dynamicWords[activeWordIndex.value] || copy.value.dynamicWords[0])
const heroGlowStyle = computed(() => ({
  '--hero-glow-x': heroGlow.value.x,
  '--hero-glow-y': heroGlow.value.y
}))

function goSearch() {
  router.push({ name: 'animals', query: { keyword: keyword.value } })
}

function openRescueDetail(rescueId) {
  router.push({ name: 'rescues', query: { detail: rescueId } })
}

function handleHeroMove(event) {
  const rect = event.currentTarget.getBoundingClientRect()
  const localX = event.clientX - rect.left
  const localY = event.clientY - rect.top
  const x = `${(localX / rect.width) * 100}%`
  const y = `${(localY / rect.height) * 100}%`
  heroGlow.value = { x, y }

  const now = Date.now()
  if (now - lastParticleAt < 55) {
    return
  }
  lastParticleAt = now

  const id = particleId++
  const duration = 900 + Math.round(Math.random() * 500)
  const driftX = `${(Math.random() - 0.5) * 90}px`
  const driftY = `${-40 - Math.random() * 90}px`
  const size = `${8 + Math.round(Math.random() * 10)}px`
  const opacity = (0.28 + Math.random() * 0.34).toFixed(2)

  heroParticles.value = [
    ...heroParticles.value.slice(-16),
    {
      id,
      style: {
        left: `${localX}px`,
        top: `${localY}px`,
        '--particle-x': driftX,
        '--particle-y': driftY,
        '--particle-size': size,
        '--particle-duration': `${duration}ms`,
        '--particle-opacity': opacity
      }
    }
  ]

  const timer = setTimeout(() => {
    heroParticles.value = heroParticles.value.filter((item) => item.id !== id)
    particleTimers.delete(timer)
  }, duration)
  particleTimers.add(timer)
}

function resetHeroGlow() {
  heroGlow.value = { x: '30%', y: '34%' }
}

function startPetMarqueeDrag(event) {
  if (!petMarqueeRef.value) {
    return
  }

  isPetMarqueeDragging = true
  petMarqueeStartX = event.clientX
  petMarqueeStartScroll = petMarqueeRef.value.scrollLeft
  petMarqueePauseUntil = Date.now() + 4000
  petMarqueeRef.value.classList.add('is-dragging')
  petMarqueeRef.value.setPointerCapture?.(event.pointerId)
}

function movePetMarqueeDrag(event) {
  if (!isPetMarqueeDragging || !petMarqueeRef.value) {
    return
  }

  const deltaX = event.clientX - petMarqueeStartX
  petMarqueeRef.value.scrollLeft = petMarqueeStartScroll - deltaX
}

function stopPetMarqueeDrag(event) {
  if (!isPetMarqueeDragging || !petMarqueeRef.value) {
    return
  }

  isPetMarqueeDragging = false
  petMarqueePauseUntil = Date.now() + 4000
  petMarqueeRef.value.classList.remove('is-dragging')
  petMarqueeRef.value.releasePointerCapture?.(event.pointerId)
}

function loopPetMarquee() {
  const container = petMarqueeRef.value
  if (container) {
    const halfWidth = container.scrollWidth / 2
    if (!isPetMarqueeDragging && Date.now() > petMarqueePauseUntil) {
      container.scrollLeft += 0.45
      if (container.scrollLeft >= halfWidth) {
        container.scrollLeft -= halfWidth
      }
    }
  }

  marqueeFrame = window.requestAnimationFrame(loopPetMarquee)
}

function setupRevealObserver() {
  revealObserver?.disconnect()
  if (!pageRef.value || typeof IntersectionObserver === 'undefined') {
    return
  }

  revealObserver = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add('is-visible')
        revealObserver.unobserve(entry.target)
      }
    })
  }, {
    threshold: 0.18,
    rootMargin: '0px 0px -8% 0px'
  })

  pageRef.value.querySelectorAll('.reveal-on-scroll').forEach((element) => {
    revealObserver.observe(element)
  })
}

async function loadHome() {
  try {
    const data = await homeApi.overview()
    overview.value = data.overview || demoOverview
    animals.value = data.latestAnimals?.length ? data.latestAnimals : demoAnimals
    rescues.value = data.latestRescues?.length ? data.latestRescues : demoRescues
    notices.value = data.latestNotices || []
  } catch {
    overview.value = demoOverview
    animals.value = demoAnimals
    rescues.value = demoRescues
    notices.value = []
  }
}

onMounted(async () => {
  await loadHome()
  await nextTick()
  setupRevealObserver()
  if (petMarqueeRef.value) {
    petMarqueeRef.value.scrollLeft = 0
    marqueeFrame = window.requestAnimationFrame(loopPetMarquee)
  }
  wordTimer = window.setInterval(() => {
    activeWordIndex.value = (activeWordIndex.value + 1) % copy.value.dynamicWords.length
  }, 2200)
})

onBeforeUnmount(() => {
  if (wordTimer) {
    window.clearInterval(wordTimer)
  }
  if (marqueeFrame) {
    window.cancelAnimationFrame(marqueeFrame)
  }
  revealObserver?.disconnect()
  particleTimers.forEach((timer) => clearTimeout(timer))
  particleTimers.clear()
})
</script>
