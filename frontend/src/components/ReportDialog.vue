<template>
  <el-dialog v-model="visibleModel" :title="t('report.dialogTitle')" width="560px" append-to-body>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item :label="t('report.targetType')">
        <el-input :model-value="targetLabel" disabled />
      </el-form-item>
      <el-form-item :label="t('report.reasonType')" prop="reasonType">
        <el-select v-model="form.reasonType" style="width:100%">
          <el-option v-for="item in reportReasonOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('report.description')" prop="description">
        <el-input v-model="form.description" type="textarea" :rows="4" maxlength="1000" show-word-limit />
      </el-form-item>
      <el-form-item :label="t('report.evidence')">
        <ImageUploader v-model="form.evidenceImageUrls" usage="report-evidence" :limit="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visibleModel = false">{{ t('common.cancel') }}</el-button>
      <el-button :loading="saving" type="primary" @click="submit">{{ t('report.submit') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { reportApi } from '../api'
import { notifyError } from '../api/http'
import ImageUploader from './ImageUploader.vue'
import { optionText, reportReasonOptions, reportTargetOptions } from '../utils/status'

const props = defineProps({
  modelValue: Boolean,
  targetType: { type: String, required: true },
  targetId: { type: [String, Number], required: true }
})

const emit = defineEmits(['update:modelValue', 'submitted'])

const visibleModel = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const saving = ref(false)
const formRef = ref()
const { t } = useI18n()
const form = reactive({
  reasonType: 'FALSE_INFORMATION',
  description: '',
  evidenceImageUrls: []
})

const targetLabel = computed(() => optionText(reportTargetOptions, props.targetType))

const rules = {
  reasonType: [{ required: true, message: t('report.reasonRequired'), trigger: 'change' }],
  description: [{ required: true, message: t('report.descriptionRequired'), trigger: 'blur' }]
}

watch(() => props.modelValue, (visible) => {
  if (visible) {
    form.reasonType = 'FALSE_INFORMATION'
    form.description = ''
    form.evidenceImageUrls = []
  }
})

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    await reportApi.create({
      targetType: props.targetType,
      targetId: Number(props.targetId),
      reasonType: form.reasonType,
      description: form.description,
      evidenceImageUrls: form.evidenceImageUrls
    })
    ElMessage.success(t('report.submitSuccess'))
    emit('submitted')
    visibleModel.value = false
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}
</script>
