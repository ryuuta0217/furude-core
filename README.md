# furude-core
古手鯖の専用プラグインです。Paper 1.20.2 用にデザインされています。

一部のソース・コードは、[Unknown Network](https://mc-unknown.net/) が著作権を保有しています。

メイン・クラスは [ここ](https://github.com/ryuuta0217/furude-core/blob/main/src/main/java/com/ryuuta0217/furude/FurudeCore.java) にあります。

## 機能など
* 一括破壊
* ローマ字 -> かな変換
* 任意コードの評価 (機能拡張用)

## home made (self bake) (ビルド方法)
1. このリポジトリをクローンする
2. `gradlew build jar shadowJar reobfJar` を実行する
3. `build/libs` に `furude-core.jar` が生成されるので、サーバーの plugins フォルダに入れる。