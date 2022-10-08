tsc コマンドに関する遊び場
==========

* メモ : [tsc コマンドに関する知見や困ったこと](https://scrapbox.io/nobuoka-pub/tsc_%E3%82%B3%E3%83%9E%E3%83%B3%E3%83%89%E3%81%AB%E9%96%A2%E3%81%99%E3%82%8B%E7%9F%A5%E8%A6%8B%E3%82%84%E5%9B%B0%E3%81%A3%E3%81%9F%E3%81%93%E3%81%A8)

## 説明

* src/module-bar と src/module-foo という 2 つのプロジェクトを持っている
* TypeScript 3.0 から導入された[プロジェクト参照 (Project References)](https://www.typescriptlang.org/docs/handbook/project-references.html) の機能を使っている
* src/module-bar が src/module-foo を参照しているので、`tsc --build src/module-bar` とするだけで src/module-foo もビルドされる

## 試し方

```
npm install
npm run build
npm run watch
```
