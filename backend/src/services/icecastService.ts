import { spawn } from 'child_process';
import * as fs from 'fs';
import { config } from '../config';

export class IcecastService {
  /**
   * ffmpeg를 사용하여 오디오 파일을 Icecast 서버로 스트리밍
   */
  async streamAudioFile(audioFilePath: string): Promise<void> {
    return new Promise((resolve, reject) => {
      if (!fs.existsSync(audioFilePath)) {
        reject(new Error(`Audio file not found: ${audioFilePath}`));
        return;
      }

      console.log('📡 Starting stream to Icecast server...');
      console.log(`🎵 Audio file: ${audioFilePath}`);
      console.log(`🌐 Server: ${config.icecast.host}:${config.icecast.port}${config.icecast.mount}`);

      // Icecast 스트림 URL (icecast:// 프로토콜 사용)
      const streamUrl = `icecast://${config.icecast.username}:${config.icecast.password}@${config.icecast.host}:${config.icecast.port}${config.icecast.mount}`;

      // ffmpeg 명령어 구성
      const ffmpegArgs = [
        '-re',                          // 실시간 속도로 읽기
        '-i', audioFilePath,            // 입력 파일
        '-codec:a', 'libmp3lame',      // MP3 인코더
        '-b:a', '128k',                // 비트레이트
        '-ar', '44100',                // 샘플레이트
        '-ac', '2',                    // 스테레오
        '-f', 'mp3',                   // 출력 포맷
        '-content_type', 'audio/mpeg', // 컨텐츠 타입
        streamUrl
      ];

      const ffmpeg = spawn('ffmpeg', ffmpegArgs);

      let errorOutput = '';

      ffmpeg.stderr.on('data', (data) => {
        const message = data.toString();
        errorOutput += message;
        // ffmpeg는 stderr로 진행상황을 출력함
        if (message.includes('time=')) {
          process.stdout.write(`\r⏱️  ${message.split('time=')[1]?.split(' ')[0] || ''}`);
        }
      });

      ffmpeg.on('close', (code) => {
        console.log('\n');
        if (code === 0) {
          console.log('✅ Streaming completed successfully');
          resolve();
        } else {
          console.error(`❌ ffmpeg exited with code ${code}`);
          console.error('Error output:', errorOutput);
          reject(new Error(`ffmpeg process exited with code ${code}`));
        }
      });

      ffmpeg.on('error', (error) => {
        console.error('❌ Error spawning ffmpeg:', error);
        reject(error);
      });
    });
  }

  /**
   * Icecast 서버 연결 테스트
   */
  async testConnection(): Promise<boolean> {
    try {
      const axios = (await import('axios')).default;
      const url = `http://${config.icecast.host}:${config.icecast.port}/status-json.xsl`;
      
      console.log(`🔍 Testing connection to Icecast server: ${url}`);
      const response = await axios.get(url, { timeout: 5000 });
      
      if (response.status === 200) {
        console.log('✅ Icecast server is reachable');
        return true;
      }
      
      return false;
    } catch (error) {
      console.error('❌ Cannot reach Icecast server:', error);
      return false;
    }
  }
}
