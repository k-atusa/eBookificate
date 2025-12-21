import React from 'react';
import './Schedule.css';

interface ScheduleItem {
  time: string;
  title: string;
  description: string;
  type: 'music' | 'news' | 'special';
}

const scheduleData: ScheduleItem[] = [
//   {
//     time: '02:00 - 05:30',
//     title: '방송시간 아니',
//     description: '편안한 밤을 위한 클래식 음악',
//     type: 'music',
//   },
  {
    time: '05:30 - 06:30',
    title: '모닝 뉴스',
    description: '오늘의 주요 뉴스를 전해드립니다',
    type: 'news',
  },
  {
    time: '06:30 - 09:00',
    title: '모닝 카페',
    description: '상쾌한 아침을 여는 팝송 모음',
    type: 'music',
  },
  {
    time: '09:00 - 12:00',
    title: '점심 시간의 멜로디',
    description: 'K-POP과 최신 차트 음악',
    type: 'music',
  },
  {
    time: '12:00 - 13:00',
    title: '런치타임 재즈',
    description: '점심시간을 위한 재즈 음악',
    type: 'music',
  },
  {
    time: '13:00 - 18:00',
    title: '오후의 여유',
    description: '편안한 오후를 위한 어쿠스틱 음악',
    type: 'music',
  },
  {
    time: '18:00 - 21:00',
    title: '저녁의 시간',
    description: '퇴근길을 함께하는 감성 발라드',
    type: 'music',
  },
  {
    time: '21:00 - 00:00',
    title: '나이트 라디오',
    description: '밤을 위한 따뜻한 음악',
    type: 'music',
  },
];

const Schedule: React.FC = () => {
  const getTypeColor = (type: string) => {
    switch (type) {
      case 'news':
        return '#ff6b6b';
      case 'special':
        return '#ffd93d';
      case 'music':
      default:
        return '#6bcf7f';
    }
  };

  const getTypeLabel = (type: string) => {
    switch (type) {
      case 'news':
        return '뉴스';
      case 'special':
        return '특집';
      case 'music':
      default:
        return '음악';
    }
  };

  return (
    <div className="schedule-container">
      <h2 className="schedule-title">📅 방송 시간표</h2>
      <div className="schedule-list">
        {scheduleData.map((item, index) => (
          <div key={index} className="schedule-item">
            <div className="schedule-time">{item.time}</div>
            <div className="schedule-content">
              <div className="schedule-header">
                <h3 className="schedule-program-title">{item.title}</h3>
                <span
                  className="schedule-type-badge"
                  style={{ backgroundColor: getTypeColor(item.type) }}
                >
                  {getTypeLabel(item.type)}
                </span>
              </div>
              <p className="schedule-description">{item.description}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Schedule;
