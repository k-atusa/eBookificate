import cron from 'node-cron';
import { NewsRadioService } from './services/newsRadioService';
import { config } from './config';

class RadioStation {
  private newsRadioService: NewsRadioService;

  constructor() {
    this.newsRadioService = new NewsRadioService();
  }

  /**
   * 스케줄러 시작
   */
  startScheduler(): void {
    console.log('🎙️  KATUSA Radio Station Starting...');
    console.log('📅 Broadcast Schedule:');
    console.log('   🌅 5:00 AM  - Morning News (모든 카테고리)');
    console.log('   ☀️  12:00 PM - Noon News (모든 카테고리)');
    console.log('   🌆 7:00 PM  - Evening News (모든 카테고리)\n');

    // 오전 5시 - 모든 카테고리
    cron.schedule('0 5 * * *', async () => {
      console.log('\n⏰ Morning broadcast triggered (5 AM)');
      try {
        await this.newsRadioService.broadcastMorning();
      } catch (error) {
        console.error('Failed to broadcast morning news:', error);
      }
    });

    // 오후 12시 - 모든 카테고리
    cron.schedule('0 12 * * *', async () => {
      console.log('\n⏰ Noon broadcast triggered (12 PM)');
      try {
        await this.newsRadioService.broadcastNoon();
      } catch (error) {
        console.error('Failed to broadcast noon news:', error);
      }
    });

    // 오후 7시 - 모든 카테고리
    cron.schedule('0 19 * * *', async () => {
      console.log('\n⏰ Evening broadcast triggered (7 PM)');
      try {
        await this.newsRadioService.broadcastEvening();
      } catch (error) {
        console.error('Failed to broadcast evening news:', error);
      }
    });

    console.log('✅ Scheduler started successfully\n');
  }

  /**
   * 즉시 뉴스 방송 (테스트용)
   */
  async broadcastNow(): Promise<void> {
    console.log('🎙️  Creating news MP3 from all categories...');
    await this.newsRadioService.broadcastNews();
  }
}

// 메인 실행
async function main() {
  const station = new RadioStation();

  // 명령줄 인자 확인
  const args = process.argv.slice(2);
  const command = args[0];

  switch (command) {
    case 'broadcast':
      // 즉시 방송 - 모든 카테고리
      await station.broadcastNow();
      process.exit(0);
      break;

    case 'scheduler':
    default:
      // 스케줄러 시작 (기본)
      station.startScheduler();
      
      // 프로세스 종료 핸들러
      process.on('SIGINT', () => {
        console.log('\n👋 Shutting down gracefully...');
        process.exit(0);
      });

      process.on('SIGTERM', () => {
        console.log('\n👋 Shutting down gracefully...');
        process.exit(0);
      });
      break;
  }
}

// 오류 핸들러
process.on('uncaughtException', (error) => {
  console.error('Uncaught Exception:', error);
  process.exit(1);
});

process.on('unhandledRejection', (reason, promise) => {
  console.error('Unhandled Rejection at:', promise, 'reason:', reason);
  process.exit(1);
});

// 시작
main().catch((error) => {
  console.error('Fatal error:', error);
  process.exit(1);
});
