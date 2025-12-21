# 🎙️ ASMR

오전 6시에 자동으로 뉴스를 방송하는 인터넷 라디오 방송국

## 📋 프로젝트 개요

이 프로젝트는 Icecast 서버를 이용한 인터넷 라디오 방송국입니다. 매일 오전 6시에 연합뉴스 RSS를 자동으로 가져와 Edge-TTS로 음성 합성하여 방송합니다.

### 주요 기능

- 📰 **자동 뉴스 방송**: 매일 오전 6시 연합뉴스 RSS 기반 자동 방송
- 🎙️ **Edge-TTS 음성 합성**: 한국어 TTS로 뉴스 내용 변환
- 📡 **Icecast 스트리밍**: 실시간 오디오 스트리밍
- 🌐 **웹 프론트엔드**: React 기반 라디오 플레이어 UI
- 📅 **방송 시간표**: 24시간 방송 스케줄 표시

## 🏗️ 프로젝트 구조

```
ASMR/
├── backend/              # TypeScript 백엔드 서버
│   ├── src/
│   │   ├── config/       # 설정 파일
│   │   ├── services/     # 비즈니스 로직
│   │   │   ├── rssService.ts       # RSS 파싱
│   │   │   ├── ttsService.ts       # Edge-TTS 연동
│   │   │   ├── icecastService.ts   # Icecast 스트리밍
│   │   │   └── newsRadioService.ts # 뉴스 방송 통합
│   │   ├── types/        # TypeScript 타입
│   │   └── index.ts      # 메인 엔트리포인트
│   └── package.json
│
└── frontend/             # React 프론트엔드
    ├── src/
    │   ├── components/
    │   │   ├── RadioPlayer.tsx  # 라디오 플레이어
    │   │   └── Schedule.tsx     # 방송 시간표
    │   ├── App.tsx
    │   └── main.tsx
    └── package.json
```

## 🚀 시작하기

### 사전 요구사항

1. **Node.js** (v18 이상)
2. **ffmpeg** (오디오 스트리밍용)
3. **Icecast** 서버 (이미 구축되어 있음)

#### 설치 명령어 (macOS)

```bash
# Homebrew로 ffmpeg 설치
brew install ffmpeg
```

> **참고**: Edge-TTS는 `edge-tts-universal` npm 패키지로 Node.js에서 직접 사용되므로 Python이나 pip 설치가 필요 없습니다.

### 백엔드 설정

1. 백엔드 디렉토리로 이동:
```bash
cd backend
```

2. 의존성 설치:
```bash
npm install
```

3. 환경 변수 설정:
```bash
cp .env.example .env
```

4. `.env` 파일 편집:
```env
ICECAST_HOST=server.katusa.xyz
ICECAST_PORT=7000
ICECAST_MOUNT=/stream
ICECAST_PASSWORD=your_source_password
ICECAST_USERNAME=source

NEWS_RSS_URL=https://www.yonhapnewstv.co.kr/browse/feed/
TTS_VOICE=ko-KR-SunHiNeural
NEWS_CRON=0 6 * * *
```

### 프론트엔드 설정

1. 프론트엔드 디렉토리로 이동:
```bash
cd frontend
```

2. 의존성 설치:
```bash
npm install
```

## 🎮 실행 방법

### 백엔드 실행

#### 1. 스케줄러 모드 (프로덕션)
매일 오전 6시에 자동으로 뉴스를 방송합니다.

```bash
cd backend
npm run dev
# 또는 빌드 후 실행
npm run build
npm start
```

#### 2. 즉시 방송 모드 (테스트)
지금 바로 뉴스를 방송합니다.

```bash
cd backend
npm run dev broadcast
```

#### 3. 연결 테스트 모드
Icecast 서버 연결을 테스트합니다.

```bash
cd backend
npm run dev test
```

### 프론트엔드 실행

```bash
cd frontend
npm run dev
```

브라우저에서 `http://localhost:3000` 접속

## 📡 Icecast 스트림 정보

- **서버**: server.katusa.xyz
- **포트**: 7000
- **마운트 포인트**: /stream
- **스트림 URL**: `http://server.katusa.xyz:7000/stream`

## 🎨 프론트엔드 기능

### 라디오 플레이어
- ▶️ 자동 재생
- ⏸️ 재생/정지 컨트롤
- 🔊 볼륨 조절
- 📡 라이브 스트리밍 상태 표시

### 방송 시간표
- 24시간 방송 스케줄
- 프로그램별 분류 (음악, 뉴스, 특집)
- 반응형 디자인

## 🛠️ 기술 스택

### 백엔드
- **TypeScript** - 타입 안전한 코드
- **Node.js** - 런타임 환경
- **rss-parser** - RSS 피드 파싱
- **edge-tts** - Microsoft Edge TTS 엔진
- **node-cron** - 작업 스케줄링
- **ffmpeg** - 오디오 스트리밍

### 프론트엔드
- **React 18** - UI 라이브러리
- **TypeScript** - 타입 시스템
- **Vite** - 빌드 도구
- **CSS3** - Glassmorphism 디자인

## 📝 커스터마이징

### 뉴스 소스 변경
`backend/.env` 파일에서 `NEWS_RSS_URL` 수정

### TTS 음성 변경
`backend/.env` 파일에서 `TTS_VOICE` 수정

사용 가능한 음성 목록은 [Microsoft Edge TTS 문서](https://learn.microsoft.com/en-us/azure/cognitive-services/speech-service/language-support?tabs=tts)에서 확인하세요.

한국어 음성 예시:
- `ko-KR-SunHiNeural` (여성)
- `ko-KR-InJoonNeural` (남성)

### 방송 시간 변경
`backend/.env` 파일에서 `NEWS_CRON` 수정 (cron 표현식)
- `0 6 * * *` - 매일 오전 6시
- `0 */2 * * *` - 2시간마다
- `0 8,18 * * *` - 오전 8시, 오후 6시

### 방송 시간표 수정
`frontend/src/components/Schedule.tsx`의 `scheduleData` 배열 수정

## 🐛 문제 해결

### 백엔드 이슈

**문제**: TTS 파일이 생성되지 않음
- `edge-tts-universal` 패키지가 자동으로 TTS를 처리합니다
- 인터넷 연결을 확인하세요 (Microsoft Edge TTS API 사용)
- 로그를 확인하여 오류 메시지를 찾아보세요

```bash
# 즉시 테스트로 확인
cd backend
npm run dev broadcast
```

**문제**: Icecast 연결 실패
```bash
# 연결 테스트
npm run dev test

# Icecast 서버 상태 확인
curl http://server.katusa.xyz:7000/status-json.xsl
```

**문제**: ffmpeg 오류
```bash
# ffmpeg 설치 확인
ffmpeg -version

# macOS에서 재설치
brew reinstall ffmpeg
```

### 프론트엔드 이슈

**문제**: 오디오가 재생되지 않음
- 브라우저 콘솔에서 오류 확인
- 스트림 URL이 올바른지 확인
- CORS 설정 확인 (Icecast 설정)

**문제**: 자동 재생이 안 됨
- 브라우저의 자동 재생 정책 때문일 수 있음
- 사용자가 페이지와 상호작용 후 재생됨

## 📦 배포

### 백엔드 배포
```bash
cd backend
npm run build

# PM2로 실행 (추천)
npm install -g pm2
pm2 start dist/index.js --name katusa-radio
pm2 save
pm2 startup
```

### 프론트엔드 배포
```bash
cd frontend
npm run build

# dist 폴더를 웹 서버에 배포
# Nginx, Apache, Vercel, Netlify 등 사용 가능
```

## 📄 라이선스

MIT License

## 👥 기여

이슈와 PR은 환영합니다!

## 📧 문의

문제가 있거나 질문이 있으시면 이슈를 생성해주세요.
