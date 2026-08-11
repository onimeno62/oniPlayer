from pathlib import Path
import re
p = Path('app/src/main/java/com/example/ui/viewmodel/MusicPlayerViewModel.kt')
s = p.read_text()
s2 = re.sub(r'\n        delayJob = viewModelScope\.launch \{.*?\n            skipNext\(\)\n        \}\n    \}\n\n    fun isWifiConnected', '\n    fun isWifiConnected', s, count=1, flags=re.S)
if s2 == s:
    raise SystemExit('orphan delay block not found')
if 'delayJob =' in s2 or '_playbackDelayCountdown' in s2:
    raise SystemExit('delay state still remains')
p.write_text(s2)
