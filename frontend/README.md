# ASMR - Frontend

KATUSA 라디오 방송국 웹 프론트엔드

## 기능

- 🎵 실시간 라디오 스트리밍 재생
- 🎚️ 볼륨 조절 기능
- ▶️ 재생/정지 컨트롤
- 📅 24시간 방송 시간표
- 📱 반응형 디자인

## 설치

```bash
npm install
```

## 실행

### 개발 모드
```bash
npm run dev
```

브라우저에서 `http://localhost:3000` 으로 접속하세요.

### 프로덕션 빌드
```bash
npm run build
```

빌드된 파일은 `dist` 디렉토리에 생성됩니다.

### 프로덕션 미리보기
```bash
npm run preview
```

## 스트림 URL 변경

`src/App.tsx` 파일에서 `STREAM_URL` 상수를 수정하세요:

```typescript
const STREAM_URL = 'http://server.katusa.xyz:7000/stream';
```

## 방송 시간표 수정

`src/components/Schedule.tsx` 파일의 `scheduleData` 배열을 수정하여 방송 시간표를 변경할 수 있습니다.

## 프로젝트 구조

```
frontend/
├── src/
│   ├── components/
│   │   ├── RadioPlayer.tsx      # 라디오 플레이어 컴포넌트
│   │   ├── RadioPlayer.css
│   │   ├── Schedule.tsx         # 방송 시간표 컴포넌트
│   │   └── Schedule.css
│   ├── App.tsx                  # 메인 앱 컴포넌트
│   ├── App.css
│   ├── main.tsx                 # 엔트리 포인트
│   └── index.css                # 글로벌 스타일
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
```

## 기술 스택

- **React 18** - UI 라이브러리
- **TypeScript** - 타입 안전성
- **Vite** - 빌드 도구
- **CSS3** - 스타일링 (Glassmorphism 디자인)

## 브라우저 호환성

- Chrome (최신)
- Firefox (최신)
- Safari (최신)
- Edge (최신)
