import Parser from 'rss-parser';
import { NewsItem } from '../types';

// RSS Parser 설정 - content:encoded 필드 파싱
const parser = new Parser({
  customFields: {
    item: [
      ['content:encoded', 'contentEncoded'],
    ]
  }
});

// 뉴스 카테고리 정의
const NEWS_CATEGORIES = {
  latest: 'http://www.yonhapnewstv.co.kr/browse/feed/',
  politics: 'http://www.yonhapnewstv.co.kr/category/news/politics/feed/',
  economy: 'http://www.yonhapnewstv.co.kr/category/news/economy/feed/',
  society: 'http://www.yonhapnewstv.co.kr/category/news/society/feed/',
  local: 'http://www.yonhapnewstv.co.kr/category/news/local/feed/',
  international: 'http://www.yonhapnewstv.co.kr/category/news/international/feed/',
  culture: 'http://www.yonhapnewstv.co.kr/category/news/culture/feed/',
  sports: 'http://www.yonhapnewstv.co.kr/category/news/sports/feed/',
  weather: 'http://www.yonhapnewstv.co.kr/category/news/weather/feed/',
};

const CATEGORY_NAMES = {
  latest: '최신 뉴스',
  politics: '정치',
  economy: '경제',
  society: '사회',
  local: '지역',
  international: '세계',
  culture: '문화연예',
  sports: '스포츠',
  weather: '날씨',
};

export class RSSService {
  /**
   * 특정 카테고리의 RSS 피드에서 뉴스 가져오기
   */
  async fetchNewsByCategory(categoryUrl: string, limit: number = 999): Promise<NewsItem[]> {
    try {
      const feed = await parser.parseURL(categoryUrl);

      const newsItems: NewsItem[] = feed.items.slice(0, limit).map((item: any) => {
        // content:encoded 필드 사용 (없으면 contentSnippet 사용)
        const contentEncoded = item.contentEncoded || item['content:encoded'] || '';
        const cleanContent = this.stripHtml(contentEncoded || item.contentSnippet || '');
        
        // description은 제목 요약용으로만 사용
        const cleanDescription = this.stripHtml(item.contentSnippet || '');

        return {
          title: item.title || '제목 없음',
          description: cleanDescription,
          link: item.link || '',
          pubDate: item.pubDate || new Date().toISOString(),
          content: cleanContent, // content:encoded의 내용
        };
      });

      return newsItems;
    } catch (error) {
      console.error(`❌ Error fetching RSS feed from ${categoryUrl}:`, error);
      return [];
    }
  }

  /**
   * 모든 카테고리의 뉴스 가져오기 (카테고리별로 지정된 개수만큼)
   */
  async fetchAllCategoryNews(limitPerCategory: number = 11): Promise<Map<string, NewsItem[]>> {
    console.log('🔍 Fetching all news from all categories...\n');
    
    const categoriesNews = new Map<string, NewsItem[]>();
    
    for (const [category, url] of Object.entries(NEWS_CATEGORIES)) {
      console.log(`📰 Fetching ${CATEGORY_NAMES[category as keyof typeof CATEGORY_NAMES]}...`);
      const news = await this.fetchNewsByCategory(url, limitPerCategory);
      if (news.length > 0) {
        categoriesNews.set(category, news);
        console.log(`   ✅ ${news.length}개 뉴스 가져옴`);
      } else {
        console.log(`   ⚠️  뉴스를 가져오지 못했습니다`);
      }
    }
    
    console.log(`\n✅ Total: ${categoriesNews.size} categories fetched\n`);
    return categoriesNews;
  }

  /**
   * 특정 카테고리들의 뉴스만 가져오기
   */
  async fetchSelectedCategories(
    categories: string[], 
    limitPerCategory: number = 999
  ): Promise<Map<string, NewsItem[]>> {
    console.log('🔍 Fetching news from selected categories...\n');
    
    const categoriesNews = new Map<string, NewsItem[]>();
    
    for (const category of categories) {
      const url = NEWS_CATEGORIES[category as keyof typeof NEWS_CATEGORIES];
      if (!url) {
        console.log(`⚠️  Unknown category: ${category}`);
        continue;
      }

      console.log(`📰 Fetching ${CATEGORY_NAMES[category as keyof typeof CATEGORY_NAMES]}...`);
      const news = await this.fetchNewsByCategory(url, limitPerCategory);
      if (news.length > 0) {
        categoriesNews.set(category, news);
        console.log(`   ✅ ${news.length}개 뉴스 가져옴`);
      } else {
        console.log(`   ⚠️  뉴스를 가져오지 못했습니다`);
      }
    }
    
    console.log(`\n✅ Total: ${categoriesNews.size} categories fetched\n`);
    return categoriesNews;
  }

  /**
   * RSS 피드에서 최신 뉴스 가져오기 (기존 호환성 유지)
   */
  async fetchLatestNews(limit: number = 5): Promise<NewsItem[]> {
    try {
      console.log(`🔍 Fetching latest news...`);
      return await this.fetchNewsByCategory(NEWS_CATEGORIES.latest, limit);
    } catch (error) {
      console.error('❌ Error fetching RSS feed:', error);
      throw error;
    }
  }
  /**
   * HTML 태그 제거 및 텍스트 정리
   */
  private stripHtml(html: string): string {
    // HTML 태그 제거
    let text = html.replace(/<[^>]*>/g, '');
    
    // HTML 엔티티 디코딩
    text = text
      .replace(/&nbsp;/g, ' ')
      .replace(/&amp;/g, '&')
      .replace(/&lt;/g, '<')
      .replace(/&gt;/g, '>')
      .replace(/&quot;/g, '"')
      .replace(/&#39;/g, "'")
      .replace(/&apos;/g, "'");
    
    // 여러 공백을 하나로
    text = text.replace(/\s+/g, ' ').trim();
    
    return text;
  }

  /**
   * 뉴스를 TTS용 스크립트로 포맷팅
   */
  formatNewsForTTS(newsItems: NewsItem[]): string {
    const intro = '안녕하세요. 카투사 라디오 방송국입니다. 지금부터 오늘의 주요 뉴스를 전해드리겠습니다.';
    
    const newsScripts = newsItems.map((item, index) => {
      return `${index + 1}번째 뉴스입니다. ${item.title}. ${item.content}`;
    });

    const outro = '이상 오늘의 뉴스를 마치겠습니다. 좋은 하루 되세요.';

    return [intro, ...newsScripts, outro].join('\n\n');
  }

  /**
   * 모든 카테고리의 뉴스를 TTS용 스크립트로 포맷팅
   */
  formatAllCategoryNewsForTTS(categoriesNews: Map<string, NewsItem[]>): string {
    const intro = '안녕하세요. 카투사 라디오 방송국입니다. 지금부터 오늘의 뉴스를 카테고리별로 전해드리겠습니다.';
    
    const categoryScripts: string[] = [];
    
    for (const [category, newsItems] of categoriesNews.entries()) {
      if (newsItems.length === 0) continue;
      
      const categoryName = CATEGORY_NAMES[category as keyof typeof CATEGORY_NAMES];
      categoryScripts.push(`\n${categoryName} 소식입니다.`);
      
      newsItems.forEach((item, index) => {
        categoryScripts.push(`${index + 1}번. ${item.title}. ${item.content}`);
      });
    }

    const outro = '이상으로 오늘의 모든 뉴스를 마치겠습니다. 좋은 하루 되세요.';

    return [intro, ...categoryScripts, outro].join('\n\n');
  }
}
