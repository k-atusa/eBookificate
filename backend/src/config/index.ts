import dotenv from 'dotenv';
import { Config } from '../types';

dotenv.config();

export const config: Config = {
  icecast: {
    host: process.env.ICECAST_HOST || 'server.katusa.xyz',
    port: parseInt(process.env.ICECAST_PORT || '7000'),
    mount: process.env.ICECAST_MOUNT || '/stream',
    password: process.env.ICECAST_PASSWORD || 'hackme',
    username: process.env.ICECAST_USERNAME || 'source',
  },
  news: {
    rssUrl: process.env.NEWS_RSS_URL || 'https://www.yonhapnewstv.co.kr/browse/feed/',
  },
  tts: {
    voice: process.env.TTS_VOICE || 'ko-KR-SunHiNeural',
    rate: process.env.TTS_RATE || '+0%',
    volume: process.env.TTS_VOLUME || '+0%',
  },
  schedule: {
    newsCron: process.env.NEWS_CRON || '0 5 * * *', // 매일 오전 5시
  },
};
