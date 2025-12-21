import * as fs from 'fs';
import * as path from 'path';
import { EdgeTTS } from 'edge-tts-universal';
import { config } from '../config';

export class TTSService {
  private outputDir: string;

  constructor() {
    this.outputDir = path.join(__dirname, '../../temp');
    this.ensureOutputDir();
  }

  /**
   * 출력 디렉토리 생성
   */
  private ensureOutputDir(): void {
    if (!fs.existsSync(this.outputDir)) {
      fs.mkdirSync(this.outputDir, { recursive: true });
    }
  }

  /**
   * 텍스트를 음성으로 변환
   */
  async textToSpeech(text: string, outputFileName: string = 'news.mp3'): Promise<string> {
    try {
      const outputPath = path.join(this.outputDir, outputFileName);
      
      // 기존 파일이 있다면 삭제
      if (fs.existsSync(outputPath)) {
        fs.unlinkSync(outputPath);
      }

      console.log('🎙️  Converting text to speech...');
      console.log(`Voice: ${config.tts.voice}`);
      
      // edge-tts-universal을 사용하여 TTS 생성
      // EdgeTTS(text, voice, options)
      const tts = new EdgeTTS(text, config.tts.voice, {
        rate: config.tts.rate,
        volume: config.tts.volume,
      });
      
      const result = await tts.synthesize();
      
      // Blob을 Buffer로 변환하여 파일로 저장
      const arrayBuffer = await result.audio.arrayBuffer();
      const buffer = Buffer.from(arrayBuffer);
      fs.writeFileSync(outputPath, buffer);

      // 파일이 생성되었는지 확인
      if (!fs.existsSync(outputPath)) {
        throw new Error('TTS file was not created');
      }

      const stats = fs.statSync(outputPath);
      console.log(`✅ TTS audio created: ${outputPath} (${this.formatBytes(stats.size)})`);

      return outputPath;
    } catch (error) {
      console.error('❌ Error converting text to speech:', error);
      throw error;
    }
  }

  /**
   * 바이트를 읽기 쉬운 형식으로 변환
   */
  private formatBytes(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  }

  /**
   * 임시 파일 삭제
   */
  cleanupTempFiles(): void {
    try {
      if (fs.existsSync(this.outputDir)) {
        const files = fs.readdirSync(this.outputDir);
        files.forEach(file => {
          const filePath = path.join(this.outputDir, file);
          fs.unlinkSync(filePath);
        });
        console.log('🧹 Cleaned up temp files');
      }
    } catch (error) {
      console.error('❌ Error cleaning up temp files:', error);
    }
  }
}
