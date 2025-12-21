import { RSSService } from '../services/rssService';
import { TTSService } from '../services/ttsService';

export class NewsRadioService {
  private rssService: RSSService;
  private ttsService: TTSService;

  constructor() {
    this.rssService = new RSSService();
    this.ttsService = new TTSService();
  }

  /**
   * 뉴스 방송 실행 - 모든 카테고리
   */
  async broadcastNews(): Promise<void> {
    console.log('\n=================================================');
    console.log('🎙️  KATUSA Radio Station - All Category News');
    console.log('=================================================\n');

    try {
      // 1. 모든 카테고리에서 뉴스 가져오기 (모든 뉴스)
      const categoriesNews = await this.rssService.fetchAllCategoryNews();
      
      if (categoriesNews.size === 0) {
        console.log('⚠️  No news items found');
        return;
      }

      // 총 뉴스 개수 계산
      let totalNews = 0;
      for (const news of categoriesNews.values()) {
        totalNews += news.length;
      }
      console.log(`📊 Total news collected: ${totalNews} items from ${categoriesNews.size} categories\n`);

      // 2. 뉴스를 TTS 스크립트로 포맷팅
      const script = this.rssService.formatAllCategoryNewsForTTS(categoriesNews);
      console.log('📰 News Script Preview:');
      console.log('---');
      console.log(script.substring(0, 300) + '...');
      console.log('---\n');

      // 3. TTS로 변환
      const audioFile = await this.ttsService.textToSpeech(script, `all_news_${Date.now()}.mp3`);

      console.log('\n✅ All category news MP3 file created successfully!\n');
      console.log(`📁 File location: ${audioFile}\n`);
      console.log(`📊 Total categories: ${categoriesNews.size}`);
      console.log(`📊 Total news items: ${totalNews}\n`);
    } catch (error) {
      console.error('\n❌ Error during news broadcast:', error);
      throw error;
    }
  }

  /**
   * 뉴스 방송 실행 - 오전 5시 (최신, 정치, 경제)
   */
  async broadcastMorning(): Promise<void> {
    console.log('\n=================================================');
    console.log('� KATUSA Radio Station - Morning News (5 AM)');
    console.log('=================================================\n');

    try {
      // 모든 카테고리의 뉴스 가져오기
      const categoriesNews = await this.rssService.fetchAllCategoryNews();
      
      if (categoriesNews.size === 0) {
        console.log('⚠️  No news items found');
        return;
      }

      let totalNews = 0;
      for (const news of categoriesNews.values()) {
        totalNews += news.length;
      }
      console.log(`📊 Total news collected: ${totalNews} items from ${categoriesNews.size} categories\n`);

      const script = this.rssService.formatAllCategoryNewsForTTS(categoriesNews);
      console.log('📰 News Script Preview:');
      console.log('---');
      console.log(script.substring(0, 300) + '...');
      console.log('---\n');

      const audioFile = await this.ttsService.textToSpeech(script, `morning_5am_${Date.now()}.mp3`);

      console.log('\n✅ Morning news MP3 created successfully!\n');
      console.log(`📁 File: ${audioFile}\n`);
      console.log(`📊 All categories included`);
      console.log(`📊 Total: ${totalNews} items\n`);
    } catch (error) {
      console.error('\n❌ Error during morning broadcast:', error);
      throw error;
    }
  }

  /**
   * 뉴스 방송 실행 - 오후 12시 (최신, 사회, 지역, 세계)
   */
  async broadcastNoon(): Promise<void> {
    console.log('\n=================================================');
    console.log('☀️  KATUSA Radio Station - Noon News (12 PM)');
    console.log('=================================================\n');

    try {
      // 모든 카테고리의 뉴스 가져오기
      const categoriesNews = await this.rssService.fetchAllCategoryNews();
      
      if (categoriesNews.size === 0) {
        console.log('⚠️  No news items found');
        return;
      }

      let totalNews = 0;
      for (const news of categoriesNews.values()) {
        totalNews += news.length;
      }
      console.log(`📊 Total news collected: ${totalNews} items from ${categoriesNews.size} categories\n`);

      const script = this.rssService.formatAllCategoryNewsForTTS(categoriesNews);
      console.log('📰 News Script Preview:');
      console.log('---');
      console.log(script.substring(0, 300) + '...');
      console.log('---\n');

      const audioFile = await this.ttsService.textToSpeech(script, `noon_12pm_${Date.now()}.mp3`);

      console.log('\n✅ Noon news MP3 created successfully!\n');
      console.log(`📁 File: ${audioFile}\n`);
      console.log(`📊 All categories included`);
      console.log(`📊 Total: ${totalNews} items\n`);
    } catch (error) {
      console.error('\n❌ Error during noon broadcast:', error);
      throw error;
    }
  }

  /**
   * 뉴스 방송 실행 - 오후 7시 (최신, 문화연예, 스포츠, 날씨)
   */
  async broadcastEvening(): Promise<void> {
    console.log('\n=================================================');
    console.log('🌆 KATUSA Radio Station - Evening News (7 PM)');
    console.log('=================================================\n');

    try {
      // 모든 카테고리의 뉴스 가져오기
      const categoriesNews = await this.rssService.fetchAllCategoryNews();
      
      if (categoriesNews.size === 0) {
        console.log('⚠️  No news items found');
        return;
      }

      let totalNews = 0;
      for (const news of categoriesNews.values()) {
        totalNews += news.length;
      }
      console.log(`📊 Total news collected: ${totalNews} items from ${categoriesNews.size} categories\n`);

      const script = this.rssService.formatAllCategoryNewsForTTS(categoriesNews);
      console.log('📰 News Script Preview:');
      console.log('---');
      console.log(script.substring(0, 300) + '...');
      console.log('---\n');

      const audioFile = await this.ttsService.textToSpeech(script, `evening_7pm_${Date.now()}.mp3`);

      console.log('\n✅ Evening news MP3 created successfully!\n');
      console.log(`📁 File: ${audioFile}\n`);
      console.log(`📊 All categories included`);
      console.log(`📊 Total: ${totalNews} items\n`);
    } catch (error) {
      console.error('\n❌ Error during evening broadcast:', error);
      throw error;
    }
  }

  /**
   * 최신 뉴스만 방송 (기존 방식)
   */
  async broadcastLatestNews(): Promise<void> {
    console.log('\n=================================================');
    console.log('🎙️  KATUSA Radio Station - Latest News Only');
    console.log('=================================================\n');

    try {
      // 1. RSS에서 최신 뉴스만 가져오기
      const newsItems = await this.rssService.fetchLatestNews(5);
      
      if (newsItems.length === 0) {
        console.log('⚠️  No news items found');
        return;
      }

      // 2. 뉴스를 TTS 스크립트로 포맷팅
      const script = this.rssService.formatNewsForTTS(newsItems);
      console.log('\n📰 News Script:');
      console.log('---');
      console.log(script.substring(0, 200) + '...');
      console.log('---\n');

      // 3. TTS로 변환
      const audioFile = await this.ttsService.textToSpeech(script, `latest_news_${Date.now()}.mp3`);

      console.log('\n✅ Latest news MP3 file created successfully!\n');
      console.log(`📁 File location: ${audioFile}\n`);
    } catch (error) {
      console.error('\n❌ Error during news broadcast:', error);
      throw error;
    }
  }
}
