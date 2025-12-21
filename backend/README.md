# KATUSA Radio Station - Backend

뉴스 RSS를 자동으로 수집하고 TTS로 변환하여 MP3 파일을 생성하는 라디오 방송국 백엔드

## 방송 스케줄

자동 스케줄러는 하루 3번 모든 카테고리의 뉴스를 생성합니다:

- **🌅 오전 5시** - Morning News (모든 카테고리)
- **☀️ 오후 12시** - Noon News (모든 카테고리)
- **🌆 오후 7시** - Evening News (모든 카테고리)

## 기능

- 📰 연합뉴스 RSS 피드에서 뉴스 자동 수집 (9개 카테고리)
- 🎙️ Edge-TTS를 사용한 한국어 음성 합성
- � MP3 파일 자동 생성
- ⏰ 하루 3번 자동 방송 (5시, 12시, 19시)

## 설치

```bash
npm install
```

## 설정

`.env` 파일을 생성하고 다음 내용을 설정하세요:

```env
# TTS 설정
TTS_VOICE=ko-KR-SunHiNeural
TTS_RATE=+0%
TTS_VOLUME=+0%

# 뉴스 RSS URL
NEWS_RSS_URL=https://www.yonhapnewstv.co.kr/browse/feed/

# 스케줄 설정
NEWS_CRON=0 5 * * *
```

## 사전 요구사항

시스템에 다음이 설치되어 있어야 합니다:

- **Node.js** (v18 이상)

> **참고**: edge-tts는 `edge-tts-universal` npm 패키지로 자동 설치되므로 별도의 Python 설치가 필요 없습니다.

## 실행

### 1. 스케줄러 모드 (자동 방송)

매일 정해진 시간에 자동으로 뉴스 MP3를 생성합니다.

```bash
npm run dev scheduler
# 또는
npm start
```

### 2. 수동 테스트 (즉시 방송)

모든 카테고리에서 각각 11개씩 뉴스를 가져와 MP3를 생성합니다:

```bash
# 모든 카테고리 방송 (각 카테고리당 11개)
npm run dev broadcast
```

각 카테고리에서 11개씩, 총 9개 카테고리 × 11개 = 약 99개의 뉴스가 포함됩니다.

## 생성된 파일

MP3 파일은 `/temp` 디렉토리에 저장됩니다:

- `morning_5am_[timestamp].mp3` - 오전 5시 뉴스
- `noon_12pm_[timestamp].mp3` - 오후 12시 뉴스
- `evening_7pm_[timestamp].mp3` - 오후 7시 뉴스
- `all_news_[timestamp].mp3` - 모든 카테고리 뉴스

## 뉴스 카테고리

- 최신 뉴스 (latest)
- 정치 (politics)
- 경제 (economy)
- 사회 (society)
- 지역 (local)
- 세계 (international)
- 문화연예 (culture)
- 스포츠 (sports)
- 날씨 (weather)

## 프로젝트 구조

```
backend/
├── src/
│   ├── config/          # 설정 파일
│   ├── services/        # 비즈니스 로직
│   │   ├── rssService.ts
│   │   ├── ttsService.ts
│   │   ├── icecastService.ts
│   │   └── newsRadioService.ts
│   ├── types/           # TypeScript 타입 정의
│   └── index.ts         # 메인 엔트리포인트
├── temp/                # 임시 오디오 파일
├── package.json
└── tsconfig.json
```

## 개발

```bash
# TypeScript 컴파일
npm run build

# Watch 모드로 개발
npm run watch
```

## 사용된 주요 라이브러리

- **edge-tts-universal**: Node.js용 Microsoft Edge TTS (Python 불필요)
- **rss-parser**: RSS 피드 파싱
- **node-cron**: 작업 스케줄링
- **axios**: HTTP 클라이언트
- **ffmpeg**: 오디오 스트리밍 (시스템 설치 필요)
