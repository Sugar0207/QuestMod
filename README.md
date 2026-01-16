QuestMod (Shuga Quests)
=======

Minecraft (NeoForge) 向けのクエストシステム Mod。JSON でクエストを定義し、
達成状況をサーバー側で保存、クライアントへ同期して表示します。

主な機能
=========
- クエスト定義を JSON で管理（通常 / 日替わり）
- 進捗はプレイヤーごとに保存され、ログイン時に同期
- クエスト画面（G キー）で一覧・詳細・進捗・報酬を確認
- 完了/更新時の HUD 通知と効果音
- サーバー側ローカライズ（`config/shuga_quests/lang`）

導入・開発メモ
=========
- IDE は IntelliJ IDEA / Eclipse を想定
- 依存が欠けている場合は `gradlew --refresh-dependencies`
- キャッシュや生成物の整理は `gradlew clean`

使い方
=========
- クエスト画面を開く: G キー
- 管理コマンド（OP 権限）
  - `/questadmin reload` クエスト/言語の再読み込み
  - `/questadmin list` 読み込み済みクエスト ID 一覧
  - `/questadmin grant <player> <quest_id|all>` 進捗付与
  - `/questadmin reset <player> [quest_id|all]` 進捗リセット
  - `/questadmin daily reroll` 日替わり再抽選

設定/データ配置
=========
- `config/shuga_quests/quests` 通常クエスト JSON
- `config/shuga_quests/daily` 日替わり候補クエスト JSON
- `config/shuga_quests/lang` サーバー側言語 JSON

Quest Criteria Debug Checklist
=========
- [x] item_acquired: `debug.item_pickup_dirt` (pick up 8 dirt)
- [x] item_crafted: `debug.craft_table` (craft 1 crafting table)
- [x] block_broken: `debug.break_stone` (break 10 stone)
- [x] entity_killed: `debug.kill_skeletons` (kill 3 skeletons)
- [x] location_reached: `debug.overworld_height` (Overworld Y>=100)
- [ ] custom_event: no event source wired yet (not testable)

現状で実装済みの仕様
=========
- クエスト定義は `config/shuga_quests/quests` と `config/shuga_quests/daily` の JSON を読み込む
- クエスト構造: `id`（"/" は "." に置換）, `title_key`, `description_key`, `category`, `type`, `repeatable`, `objectives`, `rewards`
- 目標は AND/OR ロジック対応、criteria の進捗カウントで完了判定
- criteria 対応: item_acquired / item_crafted / block_broken / entity_killed / location_reached / custom_event
- 進捗イベントはブロック破壊・アイテム拾得・クラフト・敵撃破・定期位置チェック（40tick）で更新
- 報酬タイプ: item / xp / effect / command / advancement（完了時に付与、repeatable 対応）
- プレイヤーごとの進捗を SavedData に永続化し、フル/差分同期でクライアントへ送信
- 日替わりクエスト: UTC 日付ごとに 3 件選出、保存・再抽選・同期に対応
- 管理コマンド `/questadmin`（reload, list, grant, reset, daily reroll）
- クエスト画面: Gキーで起動、カテゴリ（All/Life/Explore/Combat）でフィルタ、詳細/進捗/報酬表示
- HUD 通知: 更新/完了時に短時間の通知表示＋効果音
- サーバー側言語ファイル `config/shuga_quests/lang` を読み込み、プレイヤーごとにロケール対応
- サンプル/デバッグクエスト: life_first_log, life_tools_ready, explore_high_place, combat_kill_zombies, debug_*

今後追加したい仕様（候補）
=========
- custom_event を発火できるイベントソース/API の追加
- HUD 通知のオン/オフを設定で切り替えられるようにする
- 日替わりクエストの UI 表示（画面内で区別・ハイライトなど）
