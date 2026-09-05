#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成 App 的推送提醒铃声，iOS 与 Android 共用同一套合成音色。

全部为程序合成，不含任何第三方素材，无版权问题。

    iOS     -> iosApp/wxpusher/WxPusher-iOS/Sounds/*.caf
               10 个：5 个短音 + 5 个预渲染的「持续 N 秒」长音。
               APNs 的 aps.sound 只认 Bundle 里的文件名，长度只能靠文件本身。
    Android -> androidApp/src/androidMain/res/raw/*.wav
               只要 5 个短音。Android 的提醒是 App 自己用 MediaPlayer 播的，
               任意时长靠循环重播实现，不需要预渲染长音。

用法：
    python3 gen_notification_sounds.py                        # 两端全部生成
    python3 gen_notification_sounds.py --platform android     # 只生成 Android
    python3 gen_notification_sounds.py --platform ios ding    # 只生成 iOS 的 ding

依赖：Python 标准库 + macOS 自带的 afconvert。
"""

import argparse
import math
import os
import struct
import subprocess
import sys
import tempfile
import wave

SAMPLE_RATE = 44100
# 留够头部空间，多个分音叠加时不至于削顶
PEAK = 0.82

#脚本已经从 iosApp/tools 挪到仓库根的 tools/，路径都相对仓库根算
REPO_ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")

IOS_OUT_DIR = os.path.join(
    REPO_ROOT, "iosApp", "wxpusher", "WxPusher-iOS", "Sounds",
)
ANDROID_OUT_DIR = os.path.join(
    REPO_ROOT, "androidApp", "src", "androidMain", "res", "raw",
)


def _blank(duration):
    return [0.0] * int(SAMPLE_RATE * duration)


def _mix(buf, start_sec, samples):
    """把 samples 叠加到 buf 的 start_sec 位置，越界部分丢弃。"""
    offset = int(start_sec * SAMPLE_RATE)
    for i, v in enumerate(samples):
        idx = offset + i
        if 0 <= idx < len(buf):
            buf[idx] += v


def partial(freq, duration, amp=1.0, tau=0.15, attack=0.004, freq_end=None):
    """
    一个分音：正弦波 + 线性起振 + 指数衰减。
    attack 用来消除起始爆音；freq_end 非空时做频率滑音（水滴音用）。
    """
    n = int(SAMPLE_RATE * duration)
    attack_n = max(1, int(SAMPLE_RATE * attack))
    out = [0.0] * n
    phase = 0.0
    for i in range(n):
        t = i / SAMPLE_RATE
        f = freq if freq_end is None else freq + (freq_end - freq) * (i / max(1, n - 1))
        # 用相位累加而不是 sin(2*pi*f*t)，否则滑音会因频率突变产生断裂
        phase += 2.0 * math.pi * f / SAMPLE_RATE
        env = math.exp(-t / tau)
        if i < attack_n:
            env *= i / attack_n
        out[i] = amp * env * math.sin(phase)
    return out


def normalize(buf):
    peak = max((abs(v) for v in buf), default=0.0)
    if peak <= 0:
        return buf
    scale = PEAK / peak
    return [v * scale for v in buf]


def fade_out(buf, seconds=0.12):
    """收尾淡出，避免文件末尾的直流跳变产生咔哒声。"""
    n = min(len(buf), int(SAMPLE_RATE * seconds))
    for i in range(n):
        buf[len(buf) - n + i] *= (n - i) / n
    return buf


def render_master_wav(buf, wav_path):
    """把浮点缓冲写成 16bit PCM 母带 wav，两个平台的输出都从它转出来。"""
    buf = fade_out(normalize(buf))
    frames = b"".join(
        struct.pack("<h", max(-32768, min(32767, int(v * 32767)))) for v in buf
    )
    with wave.open(wav_path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SAMPLE_RATE)
        w.writeframes(frames)
    return len(buf) / SAMPLE_RATE


def write_caf(name, buf):
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        wav_path = tmp.name
    try:
        seconds = render_master_wav(buf, wav_path)

        os.makedirs(IOS_OUT_DIR, exist_ok=True)
        caf_path = os.path.normpath(os.path.join(IOS_OUT_DIR, name + ".caf"))
        # IMA4(ADPCM) 是 Apple 明确支持的通知音格式之一，相对 16bit PCM 是 4:1。
        # 长铃声按 PCM 存会有好几 MB，对 App 包体积不可接受。
        subprocess.run(
            ["afconvert", "-f", "caff", "-d", "ima4@44100", "-c", "1",
             wav_path, caf_path],
            check=True,
        )
        size = os.path.getsize(caf_path)
        snr = measure_snr(wav_path, caf_path)
        print("  -> %s  (%.2fs, %d bytes, SNR %.1f dB)"
              % (caf_path, seconds, size, snr))
        if snr < 30.0:
            print("     !! SNR 偏低，ADPCM 量化噪声可能可闻，考虑换回 PCM 或降采样")
    finally:
        os.unlink(wav_path)


def write_android_wav(name, buf):
    """
    Android 直接用 16bit PCM wav。
    没走 ogg/mp3 是因为 macOS 自带的 afconvert 编不出这两种格式，而只要 5 个
    1 秒以内的短音，PCM 合计也就几百 KB，没必要为此引入 ffmpeg 依赖。
    """
    os.makedirs(ANDROID_OUT_DIR, exist_ok=True)
    wav_path = os.path.normpath(os.path.join(ANDROID_OUT_DIR, name + ".wav"))
    seconds = render_master_wav(buf, wav_path)
    print("  -> %s  (%.2fs, %d bytes)"
          % (wav_path, seconds, os.path.getsize(wav_path)))


def read_pcm16_data(path):
    """
    直接从 RIFF 里取 data chunk 的裸字节。
    不用 wave 模块是因为 afconvert 解码出来的是 WAVE_FORMAT_EXTENSIBLE(0xFFFE)，
    标准库的 wave 不认这个 tag，但里面的采样数据就是普通 16bit PCM。
    """
    with open(path, "rb") as f:
        raw = f.read()
    if raw[0:4] != b"RIFF" or raw[8:12] != b"WAVE":
        raise ValueError("不是 WAVE 文件: " + path)
    pos = 12
    while pos + 8 <= len(raw):
        cid = raw[pos:pos + 4]
        size = struct.unpack("<I", raw[pos + 4:pos + 8])[0]
        body = pos + 8
        if cid == b"data":
            return raw[body:body + size]
        pos = body + size + (size & 1)   # chunk 按偶数字节对齐
    raise ValueError("没找到 data chunk: " + path)


def measure_snr(src_wav, caf_path):
    """
    把编码后的 caf 解回 PCM，和原始 PCM 逐样本比对算信噪比。
    用来客观判断 ADPCM 压缩有没有在长衰减尾音上引入可闻噪声，而不是靠拍脑袋。
    """
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        back_path = tmp.name
    try:
        subprocess.run(
            ["afconvert", "-f", "WAVE", "-d", "LEI16@44100", "-c", "1",
             caf_path, back_path],
            check=True, capture_output=True,
        )
        fa = read_pcm16_data(src_wav)
        fb = read_pcm16_data(back_path)
        n = min(len(fa), len(fb)) // 2
        sa = struct.unpack("<%dh" % n, fa[: n * 2])
        sb = struct.unpack("<%dh" % n, fb[: n * 2])
        sig = sum(float(v) * v for v in sa)
        err = sum((float(x) - y) ** 2 for x, y in zip(sa, sb))
        if err <= 0:
            return 99.0
        return 10.0 * math.log10(sig / err)
    finally:
        os.unlink(back_path)


# ---------------- 各个铃声 ----------------

def make_ding():
    """叮咚：经典门铃，E6 -> C6 两声下行。"""
    buf = _blank(1.0)
    for start, freq in ((0.0, 1318.5), (0.26, 1046.5)):
        _mix(buf, start, partial(freq, 0.72, 0.60, tau=0.20))
        _mix(buf, start, partial(freq * 2, 0.72, 0.16, tau=0.11))
        _mix(buf, start, partial(freq * 3.01, 0.72, 0.05, tau=0.07))
    return buf


def make_bell():
    """清脆：G6 单击，带非整数倍分音，接近真实钟体的音色。"""
    buf = _blank(0.85)
    base = 1568.0
    # 非整数倍频比是金属钟"不谐和但好听"的来源
    for ratio, amp, tau in (
        (1.00, 0.60, 0.26),
        (2.00, 0.24, 0.17),
        (2.76, 0.16, 0.11),
        (5.40, 0.07, 0.06),
    ):
        _mix(buf, 0.0, partial(base * ratio, 0.85, amp, tau=tau, attack=0.002))
    return buf


def make_chime():
    """提示：C6-E6-G6 上行琶音，音符间有重叠，听起来是一串而不是三下。"""
    buf = _blank(1.05)
    for start, freq in ((0.0, 1046.5), (0.15, 1318.5), (0.30, 1568.0)):
        _mix(buf, start, partial(freq, 0.75, 0.52, tau=0.24))
        _mix(buf, start, partial(freq * 2, 0.75, 0.13, tau=0.13))
    return buf


def make_alarm():
    """警报：两个音高交替 4 段，加三次谐波让音色更尖、更有紧迫感。"""
    buf = _blank(0.92)
    seg = 0.17
    for i, freq in enumerate((880.0, 1174.7, 880.0, 1174.7)):
        start = i * (seg + 0.035)
        # tau 接近段长 => 每段几乎是平顶音，形成"嘀-嘟-嘀-嘟"
        _mix(buf, start, partial(freq, seg, 0.50, tau=0.30, attack=0.006))
        _mix(buf, start, partial(freq * 3, seg, 0.14, tau=0.20, attack=0.006))
    return buf


def make_drop():
    """水滴：快速下滑音 + 短共鸣尾巴。"""
    buf = _blank(0.55)
    # 主体：1700Hz 极快滑到 430Hz，这个下滑就是"滴"的听感来源
    _mix(buf, 0.0, partial(1700.0, 0.16, 0.70, tau=0.055,
                           attack=0.001, freq_end=430.0))
    # 尾巴：落点附近的共鸣，模拟水面余波
    _mix(buf, 0.05, partial(430.0, 0.45, 0.28, tau=0.10, attack=0.004))
    _mix(buf, 0.05, partial(860.0, 0.45, 0.08, tau=0.07, attack=0.004))
    return buf


# ---------------- 持续提醒铃声 ----------------
#
# 设计：一个短动机 + 静音间隔循环若干次，而不是一段连续长音 —— 连续长音听久了烦躁，
# 且没有"节拍"可辨识。这是真实来电铃声的结构。
#
# 五个时长共用同一个动机（A 大三和弦上行琶音 A5-C#6-E6），用户不论选几秒听到的都是
# 同一个"WxPusher 的声音"，区别只在持续多久。
#
# 两条施压手段：
#   1. 音量从 60%~85% 线性升到 100%（早注意到的人不会被后段轰炸，没注意到的更容易察觉）
#   2. 10s 及以上的版本重复间隔逐渐缩短（越到后面越急）；3s/5s 太短，保持等间隔

#动机的名义长度，用来排布重复的起点；实际尾音会自然延续到间隔里，这是钟声该有的样子
MOTIF_LEN = 0.85
#动机三个音的起始偏移
MOTIF_NOTES = ((0.00, 880.00),      # A5
               (0.18, 1108.73),     # C#6
               (0.36, 1318.51))     # E6


def motif(amp):
    """一次三音上行琶音。每个音渲染 1.0s 让尾音自然拖进间隔，越界部分由 _mix 丢弃。"""
    out = [0.0] * int(SAMPLE_RATE * 1.4)
    for offset, freq in MOTIF_NOTES:
        _mix(out, offset, partial(freq, 1.0, amp * 0.60, tau=0.20))
        _mix(out, offset, partial(freq * 2, 1.0, amp * 0.15, tau=0.12))
    return out


def build_periods(total, count, shrink):
    """
    算出每次重复的起始时刻。
    末次动机必须正好在 total 处结束，所以 count-1 个间隔之和 = total - MOTIF_LEN。
    shrink 是"末间隔/首间隔"，1.0 表示等间隔，小于 1 表示逐渐加密。
    """
    span = total - MOTIF_LEN
    mean = span / (count - 1)
    first = 2.0 * mean / (1.0 + shrink)
    last = first * shrink
    #间隔不能短于动机本身，否则前一次还没响完下一次就压上来了
    assert last >= MOTIF_LEN, "间隔 %.3f 短于动机 %.2f" % (last, MOTIF_LEN)

    starts = [0.0]
    t = 0.0
    for i in range(count - 1):
        # count-1 个间隔从 first 线性收缩到 last
        ratio = 0.0 if count <= 2 else i / (count - 2)
        t += first + (last - first) * ratio
        starts.append(t)
    return starts


def make_long(total, count, shrink, amp_start):
    buf = _blank(total)
    starts = build_periods(total, count, shrink)
    for i, start in enumerate(starts):
        #音量从 amp_start 线性爬到 1.0
        amp = amp_start + (1.0 - amp_start) * (i / max(1, count - 1))
        _mix(buf, start, motif(amp))
    return buf


#              总时长, 重复次数, 末间隔/首间隔, 起始音量
LONG_SPECS = {
    "wxp_long3":  (3.0,  3,  1.00, 0.85),
    "wxp_long5":  (5.0,  4,  1.00, 0.80),
    "wxp_long10": (10.0, 7,  0.55, 0.70),
    "wxp_long15": (15.0, 9,  0.55, 0.65),
    "wxp_long20": (20.0, 11, 0.55, 0.60),
}


#短音：两端都要
SHORT_BUILDERS = {
    "wxp_ding": make_ding,
    "wxp_bell": make_bell,
    "wxp_chime": make_chime,
    "wxp_alarm": make_alarm,
    "wxp_drop": make_drop,
}

BUILDERS = dict(SHORT_BUILDERS)
for _name, _spec in LONG_SPECS.items():
    BUILDERS[_name] = (lambda spec: lambda: make_long(*spec))(_spec)


def main():
    parser = argparse.ArgumentParser(
        description="生成 iOS/Android 的推送提醒铃声",
    )
    parser.add_argument(
        "--platform", choices=("ios", "android", "all"), default="all",
        help="生成哪个平台的音频，默认两端都生成",
    )
    parser.add_argument(
        "names", nargs="*",
        help="只生成指定的铃声（可省略 wxp_ 前缀），不传表示该平台的全部",
    )
    args = parser.parse_args()

    do_ios = args.platform in ("ios", "all")
    do_android = args.platform in ("android", "all")

    if args.names:
        names = []
        for w in args.names:
            key = w if w.startswith("wxp_") else "wxp_" + w
            if key not in BUILDERS:
                print("未知铃声: %s，可选: %s" % (w, ", ".join(BUILDERS)))
                return 1
            names.append(key)
    else:
        names = list(BUILDERS)

    for name in names:
        #长音只有 iOS 需要：Android 的时长是 App 循环重播短音实现的
        is_short = name in SHORT_BUILDERS
        targets = []
        if do_ios:
            targets.append(write_caf)
        if do_android and is_short:
            targets.append(write_android_wav)
        if not targets:
            continue

        print("生成 %s ..." % name)
        #合成一次，两个平台共用同一份缓冲，保证音色完全一致
        buf = BUILDERS[name]()
        for write in targets:
            write(name, list(buf))
    return 0


if __name__ == "__main__":
    sys.exit(main())
