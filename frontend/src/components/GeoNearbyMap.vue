<template>
  <section class="geo-panel">
    <div class="geo-toolbar lift-card">
      <div class="geo-title">
        <h1>{{ t('geo.title') }}</h1>
        <p>{{ t('geo.description') }}</p>
      </div>

      <div class="geo-controls">
        <el-form label-position="top" class="geo-control-form">
          <el-form-item :label="t('geo.distanceRange')">
            <el-select v-model="distanceKm" size="large" style="width: 150px" @change="handleDistanceChange">
              <el-option v-for="item in distanceOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>

          <el-form-item :label="t('geo.displayType')">
            <el-checkbox-group v-model="visibleTypes" @change="refreshMarkers">
              <el-checkbox-button label="animal">{{ t('geo.animals') }}</el-checkbox-button>
              <el-checkbox-button label="station">{{ t('geo.stations') }}</el-checkbox-button>
            </el-checkbox-group>
          </el-form-item>

          <el-form-item :label="t('geo.currentLocation')">
            <div class="location-line">
              <el-tag v-if="hasCurrentLocation" type="success" effect="plain">
                {{ currentLocation.latitude.toFixed(6) }}, {{ currentLocation.longitude.toFixed(6) }}
              </el-tag>
              <el-tag v-else type="info" effect="plain">{{ t('geo.notLocated') }}</el-tag>
              <el-button type="primary" :loading="locating" @click="locateAndLoad">{{ t('geo.viewNearby') }}</el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <el-alert
      v-if="permissionTip"
      :title="permissionTip"
      type="warning"
      show-icon
      :closable="true"
      class="permission-alert"
      @close="permissionTip = ''"
    />

    <div class="manual-card lift-card">
      <div class="manual-head">
        <strong>{{ t('geo.manualLocation') }}</strong>
        <span>{{ t('geo.manualHint') }}</span>
      </div>
      <el-form label-position="top" class="manual-form">
        <el-row :gutter="12">
          <el-col :xs="24" :sm="10">
            <el-form-item :label="t('geo.address')">
              <el-input v-model="manual.address" :placeholder="t('geo.addressPlaceholder')" clearable @keyup.enter="locateByAddress" />
            </el-form-item>
          </el-col>
          <el-col :xs="12" :sm="5">
            <el-form-item :label="t('geo.latitude')">
              <el-input-number v-model="manual.latitude" :precision="7" :step="0.0001" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="12" :sm="5">
            <el-form-item :label="t('geo.longitude')">
              <el-input-number v-model="manual.longitude" :precision="7" :step="0.0001" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="4" class="manual-actions">
            <el-button :loading="geocoding" @click="locateByAddress">{{ t('geo.locateByAddress') }}</el-button>
            <el-button type="primary" @click="applyManualCoordinate">{{ t('geo.useCoordinate') }}</el-button>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <div class="geo-content">
      <aside class="geo-list lift-card" v-loading="loading">
        <div class="list-head">
          <div>
            <strong>{{ t('geo.nearbyResults') }}</strong>
            <span>{{ t('geo.resultCount', { count: filteredPoints.length }) }}</span>
          </div>
          <el-button :icon="RefreshCw" circle @click="loadNearby" />
        </div>

        <el-scrollbar class="list-scroll">
          <div class="list-group">
            <button
              v-for="item in filteredPoints"
              :key="item.key"
              :ref="(el) => setItemRef(el, item.key)"
              class="nearby-card"
              :class="[item.type, { active: selectedKey === item.key }]"
              @click="focusPoint(item, true)"
            >
              <span class="type-badge">{{ item.type === 'animal' ? t('geo.animalPoint') : t('geo.stationPoint') }}</span>
              <strong>{{ item.title }}</strong>
              <small>{{ item.address || t('geo.noAddress') }}</small>
              <span class="distance">{{ formatDistance(item.distanceKm) }}</span>
              <span v-if="item.subtitle" class="subtitle">{{ item.subtitle }}</span>
              <el-button
                v-if="item.type === 'station' && auth.isAdmin.value"
                size="small"
                text
                type="primary"
                class="station-edit-btn"
                @click.stop="openStationLocationDialog(item)"
              >
                {{ t('geo.editLocation') }}
              </el-button>
            </button>
          </div>
          <el-empty v-if="!loading && !filteredPoints.length" :description="t('geo.noNearby')" />
        </el-scrollbar>
      </aside>

      <main class="geo-map lift-card">
        <div ref="mapContainer" class="amap-box">
          <div v-if="mapError" class="map-fallback">
            <strong>{{ t('geo.mapLoadFailed') }}</strong>
            <span>{{ mapError }}</span>
          </div>
        </div>
      </main>
    </div>

    <el-dialog v-model="stationDialogVisible" :title="t('geo.editLocationDialog')" width="560px" append-to-body>
      <el-alert :title="t('geo.adminOnlyStationLocation')" type="info" show-icon :closable="false" class="dialog-tip" />
      <el-form :model="stationForm" label-position="top">
        <el-form-item :label="t('geo.stationId')">
          <el-input v-model="stationForm.stationId" disabled />
        </el-form-item>
        <el-form-item :label="t('geo.addressDetail')">
          <el-input v-model="stationForm.addressDetail" :placeholder="t('geo.addressDetailPlaceholder')" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item :label="t('geo.latitude')">
              <el-input-number v-model="stationForm.latitude" :precision="7" :step="0.0001" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('geo.longitude')">
              <el-input-number v-model="stationForm.longitude" :precision="7" :step="0.0001" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="stationDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="savingStation" @click="saveStationLocation">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { RefreshCw } from 'lucide-vue-next'
import { locationApi } from '../api'
import { useAiAssistantPageContext } from '../composables/useAiAssistantPageContext'
import { notifyError } from '../api/http'
import { geocodeAddress, getBrowserLocation, loadAMap } from '../utils/amap'
import { useAuth } from '../stores/auth'
import { translateLabel } from '../utils/status'

const auth = useAuth()
const { t } = useI18n()
const mapContainer = ref(null)
const loading = ref(false)
const locating = ref(false)
const geocoding = ref(false)
const savingStation = ref(false)
const mapError = ref('')
const permissionTip = ref('')
const selectedKey = ref('')
const visibleTypes = ref(['animal', 'station'])
const distanceKm = ref(5)
const animals = ref([])
const stations = ref([])
const itemRefs = reactive({})
const currentLocation = reactive({ latitude: null, longitude: null })
const manual = reactive({ address: '', latitude: null, longitude: null })
const stationDialogVisible = ref(false)
const stationForm = reactive({ stationId: null, latitude: null, longitude: null, addressDetail: '' })

let AMapRef = null
let map = null
let infoWindow = null
let markers = []
let markerMap = new Map()

const distanceOptions = [
  { label: '1km', value: 1 },
  { label: '3km', value: 3 },
  { label: '5km', value: 5 },
  { label: '10km', value: 10 }
]

const hasCurrentLocation = computed(() => currentLocation.latitude !== null && currentLocation.longitude !== null)

const allPoints = computed(() => {
  const animalPoints = animals.value.map((item) => ({
    key: `animal-${item.animalId}`,
    type: 'animal',
    id: item.animalId,
    title: item.typeText
      ? `${translateLabel(item.typeText)} ${translateLabel(item.genderText || '')}`.trim()
      : t('geo.animalFallbackTitle', { id: item.animalId }),
    subtitle: translateLabel(item.statusText || ''),
    address: item.foundRegion || '',
    latitude: item.latitude,
    longitude: item.longitude,
    distanceKm: item.distanceKm,
    raw: item
  }))

  const stationPoints = stations.value.map((item) => ({
    key: `station-${item.stationId}`,
    type: 'station',
    id: item.stationId,
    title: item.stationName || t('geo.stationFallbackTitle', { id: item.stationId }),
    subtitle: item.contactPhone ? t('geo.phoneLabel', { phone: item.contactPhone }) : item.serviceTime || '',
    address: item.addressDetail || '',
    latitude: item.latitude,
    longitude: item.longitude,
    distanceKm: item.distanceKm,
    raw: item
  }))

  return [...animalPoints, ...stationPoints].sort((a, b) => Number(a.distanceKm ?? 999999) - Number(b.distanceKm ?? 999999))
})

const filteredPoints = computed(() => allPoints.value.filter((item) => visibleTypes.value.includes(item.type)))

useAiAssistantPageContext(() => ({
  pageTitle: t('geo.title'),
  pageSummary: t('geo.pageSummary'),
  viewData: {
    hasCurrentLocation: hasCurrentLocation.value,
    currentLocation: hasCurrentLocation.value ? {
      latitude: currentLocation.latitude,
      longitude: currentLocation.longitude
    } : null,
    distanceKm: distanceKm.value,
    visibleTypes: [...visibleTypes.value],
    resultCount: filteredPoints.value.length,
    visiblePoints: filteredPoints.value.slice(0, 8).map((item) => ({
      key: item.key,
      type: item.type,
      title: item.title,
      address: item.address,
      distanceKm: item.distanceKm
    }))
  }
}))

async function initMap() {
  try {
    AMapRef = await loadAMap()
    await nextTick()
    map = new AMapRef.Map(mapContainer.value, {
      zoom: 12,
      center: [114.3055, 30.5928],
      viewMode: '2D',
      resizeEnable: true
    })
    map.addControl(new AMapRef.Scale())
    map.addControl(new AMapRef.ToolBar({ position: { right: '18px', top: '18px' } }))
    infoWindow = new AMapRef.InfoWindow({ offset: new AMapRef.Pixel(0, -34) })
  } catch (error) {
    mapError.value = error.message || t('geo.mapLoadConfigError')
  }
}

async function locateAndLoad() {
  locating.value = true
  permissionTip.value = ''
  try {
    const location = await getBrowserLocation()
    setCurrentLocation(location.latitude, location.longitude)
    await loadNearby()
    ElMessage.success(t('geo.locationLoaded'))
  } catch (error) {
    permissionTip.value = error.message || t('geo.locateFailed')
    ElMessage.warning(permissionTip.value)
  } finally {
    locating.value = false
  }
}

function setCurrentLocation(latitude, longitude) {
  currentLocation.latitude = Number(latitude)
  currentLocation.longitude = Number(longitude)
  manual.latitude = currentLocation.latitude
  manual.longitude = currentLocation.longitude

  if (map) {
    map.setZoomAndCenter(14, [currentLocation.longitude, currentLocation.latitude])
  }
}

async function locateByAddress() {
  geocoding.value = true
  try {
    const location = await geocodeAddress(manual.address)
    manual.address = location.address
    setCurrentLocation(location.latitude, location.longitude)
    await loadNearby()
    ElMessage.success(t('geo.addressLoaded'))
  } catch (error) {
    notifyError(error)
  } finally {
    geocoding.value = false
  }
}

async function applyManualCoordinate() {
  if (!isValidLatitude(manual.latitude) || !isValidLongitude(manual.longitude)) {
    ElMessage.warning(t('geo.invalidCoordinate'))
    return
  }
  setCurrentLocation(manual.latitude, manual.longitude)
  await loadNearby()
}

async function handleDistanceChange() {
  if (hasCurrentLocation.value) {
    await loadNearby()
  }
}

async function loadNearby() {
  if (!hasCurrentLocation.value) {
    ElMessage.warning(t('geo.needLocateFirst'))
    return
  }

  loading.value = true
  try {
    const params = {
      latitude: currentLocation.latitude,
      longitude: currentLocation.longitude,
      distance: distanceKm.value
    }
    const [animalData, stationData] = await Promise.all([
      locationApi.nearbyAnimals(params),
      locationApi.nearbyRescueStations(params)
    ])
    animals.value = animalData?.list || []
    stations.value = stationData?.list || []
    await nextTick()
    refreshMarkers()
  } catch (error) {
    notifyError(error)
  } finally {
    loading.value = false
  }
}

function refreshMarkers() {
  if (!map || !AMapRef) return
  map.remove(markers)
  markers = []
  markerMap = new Map()

  filteredPoints.value.forEach((point) => {
    if (!isValidLatitude(point.latitude) || !isValidLongitude(point.longitude)) return

    const marker = new AMapRef.Marker({
      position: [point.longitude, point.latitude],
      title: point.title,
      content: `<div class="guitu-geo-marker ${point.type}">${point.type === 'animal' ? t('geo.animalMarker') : t('geo.stationMarker')}</div>`,
      offset: new AMapRef.Pixel(-17, -17)
    })
    marker.on('click', () => focusPoint(point, false))
    markers.push(marker)
    markerMap.set(point.key, marker)
  })

  if (markers.length) {
    map.add(markers)
    map.setFitView(markers, false, [70, 70, 70, 70], 15)
  }
}

function focusPoint(point, fromList) {
  selectedKey.value = point.key
  if (map && isValidLatitude(point.latitude) && isValidLongitude(point.longitude)) {
    map.setZoomAndCenter(15, [point.longitude, point.latitude])
    openInfoWindow(point)
  }
  if (!fromList) scrollListTo(point.key)
}

function scrollListTo(key) {
  nextTick(() => {
    itemRefs[key]?.scrollIntoView?.({ block: 'center', behavior: 'smooth' })
  })
}

function openInfoWindow(point) {
  if (!map || !infoWindow) return
  infoWindow.setContent(`
    <div class="guitu-geo-info">
      <strong>${escapeHtml(point.title)}</strong>
      <p>${escapeHtml(point.address || t('geo.noAddress'))}</p>
      <p>${escapeHtml(t('geo.distanceLabel', { distance: formatDistance(point.distanceKm) }))}</p>
      ${point.subtitle ? `<p>${escapeHtml(point.subtitle)}</p>` : ''}
    </div>
  `)
  infoWindow.open(map, [point.longitude, point.latitude])
}

function openStationLocationDialog(point) {
  Object.assign(stationForm, {
    stationId: point.id,
    latitude: point.latitude,
    longitude: point.longitude,
    addressDetail: point.address || ''
  })
  stationDialogVisible.value = true
}

async function saveStationLocation() {
  if (!isValidLatitude(stationForm.latitude) || !isValidLongitude(stationForm.longitude)) {
    ElMessage.warning(t('geo.invalidStationCoordinate'))
    return
  }

  savingStation.value = true
  try {
    await locationApi.updateRescueStationLocation({
      stationId: stationForm.stationId,
      latitude: stationForm.latitude,
      longitude: stationForm.longitude,
      addressDetail: stationForm.addressDetail
    })
    ElMessage.success(t('geo.stationUpdated'))
    stationDialogVisible.value = false
    await loadNearby()
  } catch (error) {
    notifyError(error)
  } finally {
    savingStation.value = false
  }
}

function setItemRef(el, key) {
  if (el) itemRefs[key] = el
}

function isValidLatitude(value) {
  const number = Number(value)
  return Number.isFinite(number) && number >= -90 && number <= 90
}

function isValidLongitude(value) {
  const number = Number(value)
  return Number.isFinite(number) && number >= -180 && number <= 180
}

function formatDistance(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return t('geo.unknownDistance')
  }

  const distance = Number(value)
  if (distance < 1) {
    return t('geo.distanceMetersValue', { value: Math.round(distance * 1000) })
  }

  return t('geo.distanceKilometersValue', { value: distance.toFixed(2) })
}

function escapeHtml(value) {
  return String(value || '').replace(/[&<>"']/g, (char) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  }[char]))
}

onMounted(async () => {
  await initMap()
})

onUnmounted(() => {
  if (map) {
    map.destroy()
    map = null
  }
})
</script>

<style scoped>
.geo-panel {
  display: grid;
  gap: 16px;
}

.geo-toolbar,
.manual-card,
.geo-list,
.geo-map {
  padding: 18px;
}

.geo-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.geo-title h1 {
  margin: 0 0 6px;
  font-size: clamp(24px, 3vw, 34px);
}

.geo-title p,
.manual-head span {
  margin: 0;
  color: var(--el-text-color-secondary);
}

.geo-control-form {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.geo-control-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.location-line {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.permission-alert {
  border-radius: 14px;
}

.manual-card {
  display: grid;
  gap: 12px;
}

.manual-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.manual-actions {
  display: flex;
  align-items: end;
  gap: 8px;
  padding-bottom: 18px;
}

.geo-content {
  display: grid;
  grid-template-columns: minmax(320px, 390px) minmax(0, 1fr);
  gap: 16px;
}

.geo-list {
  min-height: 620px;
}

.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.list-head div {
  display: grid;
  gap: 3px;
}

.list-head span {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.list-scroll {
  height: 560px;
}

.list-group {
  display: grid;
  gap: 10px;
  padding-right: 6px;
}

.nearby-card {
  position: relative;
  display: grid;
  gap: 5px;
  width: 100%;
  padding: 14px 76px 14px 14px;
  border: 1px solid rgba(30, 45, 60, 0.08);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.nearby-card:hover,
.nearby-card.active {
  transform: translateY(-2px);
  border-color: rgba(255, 143, 95, 0.55);
  box-shadow: 0 12px 28px rgba(30, 45, 60, 0.1);
}

.nearby-card.station.active,
.nearby-card.station:hover {
  border-color: rgba(47, 159, 124, 0.48);
}

.type-badge {
  width: fit-content;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  color: #ad5f20;
  background: rgba(255, 143, 95, 0.13);
}

.nearby-card.station .type-badge {
  color: #226e5a;
  background: rgba(47, 159, 124, 0.12);
}

.nearby-card small,
.subtitle {
  color: var(--el-text-color-secondary);
}

.distance {
  position: absolute;
  top: 14px;
  right: 14px;
  font-weight: 800;
  color: #226e5a;
}

.station-edit-btn {
  position: absolute;
  right: 10px;
  bottom: 8px;
}

.amap-box {
  position: relative;
  width: 100%;
  height: min(70vh, 720px);
  min-height: 620px;
  overflow: hidden;
  border-radius: 22px;
  background: linear-gradient(135deg, #f8efe6, #eef7f3);
}

.map-fallback {
  height: 100%;
  display: grid;
  place-content: center;
  gap: 8px;
  text-align: center;
  color: var(--el-text-color-secondary);
}

.dialog-tip {
  margin-bottom: 12px;
}

@media (max-width: 1080px) {
  .geo-toolbar {
    display: grid;
  }

  .geo-content {
    grid-template-columns: 1fr;
  }

  .geo-list {
    min-height: auto;
  }

  .list-scroll {
    height: 360px;
  }

  .amap-box {
    min-height: 460px;
    height: 56vh;
  }
}

@media (max-width: 640px) {
  .geo-toolbar,
  .manual-card,
  .geo-list,
  .geo-map {
    padding: 12px;
  }

  .geo-control-form,
  .manual-actions,
  .location-line {
    width: 100%;
  }

  .geo-control-form :deep(.el-form-item),
  .geo-control-form :deep(.el-select),
  .manual-actions .el-button,
  .location-line .el-button {
    width: 100%;
  }

  .manual-actions {
    align-items: stretch;
    padding-bottom: 0;
  }

  .nearby-card {
    padding-right: 14px;
  }

  .distance,
  .station-edit-btn {
    position: static;
  }
}
</style>

<style>
.guitu-geo-marker {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 50% 50% 50% 10%;
  transform: rotate(-45deg);
  color: white;
  font-weight: 800;
  box-shadow: 0 9px 20px rgba(33, 43, 54, 0.2);
  user-select: none;
}

.guitu-geo-marker::first-letter {
  display: inline-block;
  transform: rotate(45deg);
}

.guitu-geo-marker.animal {
  background: linear-gradient(135deg, #ff8f5f, #f25f5c);
}

.guitu-geo-marker.station {
  background: linear-gradient(135deg, #2f9f7c, #226e5a);
}

.guitu-geo-info {
  min-width: 190px;
  display: grid;
  gap: 6px;
  font-size: 13px;
}

.guitu-geo-info strong {
  color: #1e2d3c;
}

.guitu-geo-info p {
  margin: 0;
  color: #5f6b77;
}
</style>
