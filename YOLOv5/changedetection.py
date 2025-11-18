import os
import cv2
import pathlib
import requests
from datetime import datetime, timedelta
from dotenv import load_dotenv

load_dotenv()


class ChangeDetection:
    """
    - YOLO + PersonTracker 가 준 사람 ID 집합(current_ids)을 받아서
      '충분히 오래' 들어온 ID만 [입장],
      '충분히 오래' 안 보인 ID만 [퇴장] 으로 업로드.

    - 노이즈/깜빡임 때문에 같은 사람인데
      ID가 잠깐 사라졌다가 다시 보이는 경우를 막기 위한 디바운스 + 쿨다운 로직.
    """

    HOST = os.getenv("DJANGO_HOST")
    username = os.getenv("DJANGO_USERNAME")
    password = os.getenv("DJANGO_PASSWORD")

    # 몇 프레임 이상 연속으로 보여야 "입장"으로 인정할지
    ENTER_FRAMES = 8          # 대충 0.25초~0.3초 이상
    # 몇 프레임 이상 연속으로 안 보여야 "퇴장"으로 인정할지
    EXIT_FRAMES = 20          # 대충 0.7초~1초 정도
    # 같은 ID에 대해 입장/퇴장 이벤트 간 최소 시간 간격(초)
    EVENT_COOLDOWN = 2.0      # 2초 이내엔 같은 ID 이벤트 또 안 보냄

    def __init__(self):
        # 이 ID들은 "현재 시야 안에 확실히 있다"고 인정된 상태
        self.stable_ids: set[int] = set()

        # id -> 연속으로 보인 프레임 수
        self.present_frames: dict[int, int] = {}
        # id -> 연속으로 안 보인 프레임 수
        self.absent_frames: dict[int, int] = {}
        # id -> 마지막으로 이벤트(입장/퇴장) 보낸 시각
        self.last_event_time: dict[int, datetime] = {}

        self.token = None

        print("[ChangeDetection] HOST    :", self.HOST)
        print("[ChangeDetection] USERNAME:", self.username)
        print("[ChangeDetection] PASSWORD:", "(HIDDEN)" if self.password else None)

        if not self.HOST or not self.username or not self.password:
            raise RuntimeError(
                "[ChangeDetection] .env 설정(DJANGO_HOST / DJANGO_USERNAME / DJANGO_PASSWORD)을 확인하세요."
            )

        # 토큰 발급
        login_url = self.HOST + "/api-token-auth/"
        print("[Login URL]", login_url)

        res = requests.post(
            login_url,
            {
                "username": self.username,
                "password": self.password,
            },
        )
        print("[ChangeDetection] status:", res.status_code)
        print("[ChangeDetection] body  :", res.text)

        res.raise_for_status()
        self.token = res.json().get("token")
        print("[ChangeDetection] Token:", self.token)

    def add(self, current_ids, save_dir, image):
        """
        current_ids: 현재 프레임에서 PersonTracker가 준 ID 집합(set[int])
        """
        now = datetime.now()
        now_str = now.isoformat()
        time_str = now.strftime("%Y-%m-%d %H:%M:%S")

        current_ids = set(current_ids)

        # 1) 이번 프레임에서 보인 ID들: present_frames++ , absent_frames=0
        for pid in current_ids:
            self.present_frames[pid] = self.present_frames.get(pid, 0) + 1
            self.absent_frames[pid] = 0

        # 2) 이번 프레임에서 안 보인 ID들: absent_frames++ , present_frames=0
        for pid in list(self.present_frames.keys()):
            if pid not in current_ids:
                self.absent_frames[pid] = self.absent_frames.get(pid, 0) + 1
                self.present_frames[pid] = 0

        events = []  # (kind, pid) kind: "enter" or "exit"

        # 3) ENTER_FRAMES 이상 연속으로 보인 ID → 입장 이벤트
        for pid in current_ids:
            if (
                self.present_frames.get(pid, 0) >= self.ENTER_FRAMES
                and pid not in self.stable_ids
            ):
                events.append(("enter", pid))
                self.stable_ids.add(pid)

        # 4) EXIT_FRAMES 이상 연속으로 안 보인 ID → 퇴장 이벤트
        for pid in list(self.stable_ids):
            if self.absent_frames.get(pid, 0) >= self.EXIT_FRAMES:
                events.append(("exit", pid))
                self.stable_ids.remove(pid)

        # 변화가 없으면 아예 업로드 X
        if not events:
            return

        # 5) 쿨다운 체크 + 업로드
        for kind, pid in events:
            last_time = self.last_event_time.get(pid)
            if last_time is not None:
                if (now - last_time) < timedelta(seconds=self.EVENT_COOLDOWN):
                    # 너무 최근에 이미 이벤트 보냈으면 스킵
                    continue

            self.last_event_time[pid] = now

            if kind == "enter":
                title = "실시간 침입 감지 알림"
                text = f"[입장] {time_str} - 인원 ID {pid} 감지"
            else:
                title = "실시간 침입 감지 알림"
                text = f"[퇴장] {time_str} - 인원 ID {pid} 시야에서 이탈"

            self._send_one(save_dir, image, title, text, now_str)

    def _send_one(self, save_dir, image, title, text, now_str):
        """
        한 번의 POST 업로드 (이미지 1장 + 제목/본문)
        """
        now = datetime.now()

        base = pathlib.Path(os.getcwd())
        save_path = (
            base / save_dir / "detected" /
            str(now.year) / str(now.month) / str(now.day)
        )
        save_path.mkdir(parents=True, exist_ok=True)

        filename = f"{now.hour}-{now.minute}-{now.second}-{now.microsecond}.jpg"
        full_path = save_path / filename

        # 이미지 리사이즈 후 저장
        dst = cv2.resize(image, dsize=(320, 240), interpolation=cv2.INTER_AREA)
        cv2.imwrite(str(full_path), dst)

        headers = {
            "Authorization": "JWT " + self.token,
            "Accept": "application/json",
        }

        data = {
            "title": title,
            "text": text,
            "created_date": now_str,
            "published_date": now_str,
        }

        with open(full_path, "rb") as f:
            files = {"image": f}
            res = requests.post(self.HOST + "/api_root/Post/", data=data, files=files, headers=headers)

        print("[Upload]", res.status_code)
        print("[Response]", res.text)
