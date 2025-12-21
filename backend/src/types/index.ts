export interface NewsItem {
  title: string;
  description: string;
  link: string;
  pubDate: string;
  content: string;
}

export interface Config {
  icecast: {
    host: string;
    port: number;
    mount: string;
    password: string;
    username: string;
  };
  news: {
    rssUrl: string;
  };
  tts: {
    voice: string;
    rate: string;
    volume: string;
  };
  schedule: {
    newsCron: string;
  };
}
