import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import router from '@/router'
import App from '@/App.vue'

import 'primevue/resources/themes/lara-light-blue/theme.css'
import 'primevue/resources/primevue.min.css'
import 'primeicons/primeicons.css'
import 'primeflex/primeflex.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(PrimeVue, {
  ripple: true,
  locale: {
    startsWith: '시작 문자',
    contains: '포함',
    notContains: '미포함',
    endsWith: '끝 문자',
    equals: '같음',
    notEquals: '같지 않음',
    noFilter: '필터 없음',
    lt: '보다 작음',
    lte: '이하',
    gt: '보다 큼',
    gte: '이상',
    dateIs: '날짜 일치',
    dateIsNot: '날짜 불일치',
    dateBefore: '이전 날짜',
    dateAfter: '이후 날짜',
    clear: '초기화',
    apply: '적용',
    matchAll: '모두 일치',
    matchAny: '하나 이상 일치',
    addRule: '규칙 추가',
    removeRule: '규칙 삭제',
    accept: '확인',
    reject: '취소',
    choose: '선택',
    upload: '업로드',
    cancel: '취소',
    dayNames: ['일요일', '월요일', '화요일', '수요일', '목요일', '금요일', '토요일'],
    dayNamesShort: ['일', '월', '화', '수', '목', '금', '토'],
    dayNamesMin: ['일', '월', '화', '수', '목', '금', '토'],
    monthNames: ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월'],
    monthNamesShort: ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월'],
    today: '오늘',
    weekHeader: '주',
    firstDayOfWeek: 0,
    dateFormat: 'yy-mm-dd',
    weak: '약함',
    medium: '보통',
    strong: '강함',
    passwordPrompt: '비밀번호를 입력하세요',
    emptyFilterMessage: '결과가 없습니다',
    emptyMessage: '사용 가능한 옵션이 없습니다',
  },
})

app.mount('#app')
