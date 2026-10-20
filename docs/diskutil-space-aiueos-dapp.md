# app-diskutil（容量判定）— itonami（dapp）上の分散型設計

状態: 第 1 スライス実装（2026-10-20）。正本はこの文書。
追跡: Linear JUN-128（親 JUN-116）。

## 位置づけ

app-diskutil は Disk Utility —— ボリュームとその内容量。capability は
**system/metrics のみ**（満杯度の読み取りに中身を見る権限は不要 ——
app-monitor の集計 grant と同じ。**fs/browse は決して要求しない**）。

## このスライス: 容量判定コア

| 層 | 実体 | 状態 |
|---|---|---|
| 判定コア guest | `aiueos/diskutil_space.cljk` — free-space（**0 で床打ち** — used > capacity は報告ありうるが負の空きは真でない）/ percent-x10（**容量不明は 0 ではなく -1 = 不明** — ソートの安全端に置かない）/ id-is-uuid（**id は UUID、mount point は動く**）/ command-admissible（eject / copy-path のみ、**Erase は無い**）/ capability-ok（metrics のみ） | **実装済み・実測** |
| 記述子 | `aiueos/diskutil-space.edn` | **実装済み** |
| parity | `test/app_diskutil/space_parity_test.cljk` — **実物の model.cljk**（mokuroku+amu+kotoba-sema クラスパス）と一致（free-space 床打ち / percent の丸めと nil / commands / capability） | **実装済み・実測** |

実測: `amu test aiueos/diskutil_space.cljk` → **24/24**（jvm-kir / js / wasm）。
parity → 3 tests / 10 assertions / 0 failures。

## 正本の正確な規則（guest はこれに追従 —— 明文の設計意図つき）

- `free-space` は capacity - used を **max 0 で床打ち**: 「負の空き容量は真で
  なく、アプリのバグに見える」（満杯 APFS は used > capacity を報告しうる）。
- `percent-used` は capacity 不明のとき **nil（0 ではない）**: 「読めない容量の
  ボリュームは空ボリュームではなく、0% はソートの安全端に置く」。
  計算は round(1000×used/capacity)/10（1 桁小数）→ guest は 10 倍整数で保持。
- **id は UUID であって mount point ではない**: 「mount は動く。動く id は
  Eject が誤った装置を狙う」。
- コマンドは eject / copy-path のみ。**Erase は無い**: 「何もしないか最悪の
  ことをするボタンになる」。

## 未実装境界

- eject の対象確定（removable? 判定）と破壊操作の確認フロー。
- page / source の UI 面判定。
- mon（DNA）化、aiueos session 載せ（fleet 共通）。
