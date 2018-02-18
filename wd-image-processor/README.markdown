WebDriver を用いた画像処理サーバーのプロトタイプ
==========

## サンプルの動かし方

```
docker-compose up
```

で起動するはず。 Windows でしか試していないのでもしかしたらうまくいかないかも。

起動したら下記 URL にアクセスすると画像が表示される。

* http://localhost:8080/hello?width=640&height=480&arg="こんにちは"
* http://localhost:8080/map?width=640&height=480&arg=[10,10,10,15,10,20,10,25,13,30,16,33,20,35,25,36]

## 仕組み

### Docker Compose で定義されているサービス

* app : アプリケーション本体。
    * 設定ファイルで指定されたエンドポイントへのリクエストがあった際に、WebDriver を使用して指定された HTML を読み込み、JS を実行し、そのスクリーンショットを撮って返す。
* wd-server : WebDriver のブラウザ側。

### 設定ファイル

* app の `PROCESSORS_CONFIG_PATH` 環境変数で、画像処理の設定ファイルを指定する。
    * サンプル設定ファイルが [sampleProcessors/processors.json](./sampleProcessors/processors.json) にある。
    * パスをキーとして、HTML ファイルと JS ファイルを値とする JSON オブジェクト。

### クエリパラメータ

次の 3 つのクエリパラメータを取る。

* `width` : WebDriver のブラウザの画面の幅。 どうやら最小値が決まっており、438 px 未満にはできない模様。 (ChromeDriver の場合。)
* `height` : WebDriver のブラウザの画面の幅。
* `arg` : JS に渡される文字列。 JS 側では `arguments[0]` で取得できる。

## 開発する場合は

アプリケーション部分は Kotlin で書かれている。
IDE としては IntelliJ IDEA を使うのがオススメ。

## Acknowledgements

* This idea was provided by [wakaba](https://github.com/wakaba)
* Thanks to [OND Inc.](https://ond-inc.com/)
