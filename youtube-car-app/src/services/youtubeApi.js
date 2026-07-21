// YouTube API Extractor Service (Piped & Invidious API + SponsorBlock Integration)

// Public Invidious & Piped instances with automatic failover
const PIPED_INSTANCES = [
  'https://pipedapi.kavin.rocks',
  'https://api.piped.privacydev.net',
  'https://pipedapi.palvelintalo.fi',
  'https://piped-api.garudalinux.org'
];

// Helper to fetch with timeout and fallback
async function fetchWithFallback(endpoints, pathGetter) {
  for (const base of endpoints) {
    try {
      const url = pathGetter(base);
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 4000);
      const res = await fetch(url, { signal: controller.signal });
      clearTimeout(timeoutId);
      if (res.ok) {
        return await res.json();
      }
    } catch (e) {
      console.warn(`Instance failed: ${base}, trying next...`, e.message);
    }
  }
  return null;
}

// 1. Trending / Recommended Videos for Car
export async function getTrendingVideos(region = 'ID') {
  const data = await fetchWithFallback(PIPED_INSTANCES, base => `${base}/trending?region=${region}`);
  if (data && Array.isArray(data)) {
    return data.map(item => ({
      id: item.url ? item.url.replace('/watch?v=', '') : item.id,
      title: item.title,
      uploaderName: item.uploaderName || 'YouTube Creator',
      uploaderAvatar: item.uploaderAvatar || '',
      thumbnail: item.thumbnail,
      duration: item.duration || 0,
      views: item.views || 0,
      uploadedDate: item.uploadedDate || 'Terbaru'
    }));
  }

  return getFallbackCuratedVideos();
}

// 2. Search Videos
export async function searchVideos(query) {
  if (!query || query.trim() === '') return getTrendingVideos();
  const data = await fetchWithFallback(PIPED_INSTANCES, base => `${base}/search?q=${encodeURIComponent(query)}&filter=all`);
  if (data && data.items) {
    return data.items
      .filter(item => item.type === 'stream')
      .map(item => ({
        id: item.url ? item.url.replace('/watch?v=', '') : item.id,
        title: item.title,
        uploaderName: item.uploaderName || 'YouTube Creator',
        uploaderAvatar: item.uploaderAvatar || '',
        thumbnail: item.thumbnail,
        duration: item.duration || 0,
        views: item.views || 0,
        uploadedDate: item.uploadedDate || 'Baru'
      }));
  }

  return getFallbackCuratedVideos().filter(v => 
    v.title.toLowerCase().includes(query.toLowerCase()) || 
    v.uploaderName.toLowerCase().includes(query.toLowerCase())
  );
}

// 3. Get Video Stream Details (Ad-Free Streams)
export async function getVideoDetails(videoId) {
  const data = await fetchWithFallback(PIPED_INSTANCES, base => `${base}/streams/${videoId}`);
  if (data) {
    const audioStreams = data.audioStreams || [];
    const videoStreams = data.videoStreams || [];
    const hlsUrl = data.hls;

    return {
      id: videoId,
      title: data.title,
      description: data.description,
      uploader: data.uploader,
      uploaderAvatar: data.uploaderAvatar,
      views: data.views,
      likes: data.likes,
      hlsUrl: hlsUrl,
      audioStreams: audioStreams,
      videoStreams: videoStreams,
      isAdFree: true
    };
  }

  return {
    id: videoId,
    title: 'Playing Video',
    uploader: 'YouTube Channel',
    isEmbedFallback: true,
    isAdFree: true
  };
}

// 4. SponsorBlock API Integration (Auto-Skip Sponsors)
export async function getSponsorSegments(videoId) {
  try {
    const categories = '["sponsor", "intro", "outro", "selfpromo", "music_offtopic"]';
    const url = `https://sponsor.ajay.app/api/skipSegments?videoID=${videoId}&categories=${categories}`;
    const res = await fetch(url);
    if (res.ok) {
      const segments = await res.json();
      return segments.map(seg => ({
        category: seg.category,
        start: seg.segment[0],
        end: seg.segment[1]
      }));
    }
  } catch (e) {
    console.warn('SponsorBlock fetch failed:', e);
  }
  return [];
}

// Curated list for instant offline/fallback car experience
function getFallbackCuratedVideos() {
  return [
    {
      id: 'jfKfPfyJRdk',
      title: 'Lofi Hip Hop Radio 📚 - Beats to Relax/Study to',
      uploaderName: 'Lofi Girl',
      uploaderAvatar: 'https://yt3.ggpht.com/w95a_T699z-Vb5Ww3b...=s88-c-k-c0x00ffffff-no-rj',
      thumbnail: 'https://i.ytimg.com/vi/jfKfPfyJRdk/hqdefault.jpg',
      duration: 0,
      views: 125000000,
      uploadedDate: 'LIVE'
    },
    {
      id: '5qap5aO4i9A',
      title: 'Lofi Beats for Highway & Night Driving 🚗🎶',
      uploaderName: 'ChilledCow / Drive Beats',
      uploaderAvatar: '',
      thumbnail: 'https://i.ytimg.com/vi/5qap5aO4i9A/hqdefault.jpg',
      duration: 3600,
      views: 8900000,
      uploadedDate: '2 bulan lalu'
    },
    {
      id: '2g811Eo7K8U',
      title: 'Top Hits Indonesia 2026 - Lagu Santai Perjalanan Mobil',
      uploaderName: 'Indo Music Vibe',
      uploaderAvatar: '',
      thumbnail: 'https://i.ytimg.com/vi/2g811Eo7K8U/hqdefault.jpg',
      duration: 7200,
      views: 4500000,
      uploadedDate: '1 minggu lalu'
    },
    {
      id: 'DWcJFNfaw9c',
      title: 'Deep Focus & Ambient Chill out for Long Drives',
      uploaderName: 'Chill Nation',
      uploaderAvatar: '',
      thumbnail: 'https://i.ytimg.com/vi/DWcJFNfaw9c/hqdefault.jpg',
      duration: 4800,
      views: 3100000,
      uploadedDate: '3 hari lalu'
    },
    {
      id: '4xDzrJKXOOY',
      title: 'Synthwave / Retrowave Radio 24/7 - Night Drive Music',
      uploaderName: 'Lofi Girl - Synthwave',
      uploaderAvatar: '',
      thumbnail: 'https://i.ytimg.com/vi/4xDzrJKXOOY/hqdefault.jpg',
      duration: 0,
      views: 9800000,
      uploadedDate: 'LIVE'
    }
  ];
}
