import { config } from '@vue/test-utils'
import { vi } from 'vitest'

config.global.stubs = {
  'el-form': {
    template: '<form><slot /></form>',
    methods: {
      validate: vi.fn().mockResolvedValue(true),
      resetFields: vi.fn()
    }
  },
  'el-form-item': {
    template: '<div><slot /></div>'
  },
  'el-input': {
    template: '<input />',
    props: ['modelValue', 'placeholder', 'type', 'prefix-icon', 'size', 'show-password'],
    emits: ['update:modelValue']
  },
  'el-button': {
    template: '<button><slot /></button>',
    props: ['type', 'size', 'loading']
  },
  'el-table': {
    template: '<table><slot /></table>',
    props: ['data'],
    data() {
      return {
        mockData: []
      }
    }
  },
  'el-table-column': {
    template: '<td></td>',
    props: ['prop', 'label', 'width', 'min-width', 'align']
  },
  'el-pagination': {
    template: '<div class="pagination"></div>',
    props: ['current-page', 'page-size', 'total']
  },
  'el-dialog': {
    template: '<div v-if="modelValue"><slot /></div>',
    props: ['modelValue', 'title', 'width'],
    emits: ['update:modelValue']
  },
  'el-select': {
    template: '<select><slot /></select>',
    props: ['modelValue', 'placeholder', 'clearable'],
    emits: ['update:modelValue']
  },
  'el-option': {
    template: '<option><slot /></option>',
    props: ['label', 'value']
  },
  'el-date-picker': {
    template: '<input type="date" />',
    props: ['modelValue', 'type', 'placeholder', 'format', 'value-format'],
    emits: ['update:modelValue']
  },
  'el-tag': {
    template: '<span><slot /></span>',
    props: ['type']
  },
  'el-empty': {
    template: '<div class="empty">暂无数据</div>'
  },
  'el-card': {
    template: '<div class="card"><slot /></div>',
    props: ['shadow']
  },
  'el-alert': {
    template: '<div class="alert"><slot /></div>',
    props: ['type', 'closable', 'show-icon']
  },
  'el-tooltip': {
    template: '<span><slot /></span>',
    props: ['content', 'placement', 'show-after']
  },
  'el-input-number': {
    template: '<input type="number" />',
    props: ['modelValue', 'min', 'max'],
    emits: ['update:modelValue']
  },
  'el-icon': {
    template: '<i><slot /></i>'
  },
  'router-link': {
    template: '<a><slot /></a>',
    props: ['to']
  }
}

config.global.mocks = {
  $router: {
    push: vi.fn(),
    replace: vi.fn(),
    go: vi.fn(),
    back: vi.fn()
  },
  $route: {
    query: {},
    params: {}
  }
}
