package nablarch.test.core.file;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * テストデータ変換ツール（{@code nablarch.test.tool.converter}）が、本体の構造解析結果である
 * {@link DataFile}／{@link DataFileFragment} の<b>器の中身</b>を読み取るための、{@code file}
 * パッケージ相乗りのアダプタ。
 * <p>
 * {@link DataFile#all}・{@link DataFile#directives}・{@link DataFileFragment#names}／
 * {@code types}／{@code lengths}／{@code values} は protected で、変換ツールのパッケージから
 * 直接読めない。本クラスを本体と同一パッケージ（{@code nablarch.test.core.file}）に 1 枚だけ
 * 相乗りさせ、この可視性の壁を越えて読み取り専用のスナップショット（{@link FileView}）へ写す。
 * 相乗りの影響は本クラスに局所化される（設計書 §共通）。
 * </p>
 * <p>
 * 本クラスは器が保持する値を<b>そのまま</b>写すだけで、原文復元（長さ省略・型表記の生行からの復元）は
 * 行わない。器が正規化した値（型記法のフレームワーク表記化など）はそのまま現れる。原文の復元は
 * 呼び出し側（Reader）が生行から行う（設計書 §共通「器が正規化する値の原文復元」）。なお
 * レコード種別（{@link DataFileFragment#recordType}）は本体で private のため読めず、{@link FragmentView}
 * は保持しない。レコード種別も呼び出し側が生行（名前行の先頭セル）から取る。
 * </p>
 *
 * @author kiyobot
 */
public final class TestCoreFileAdapter {

    /** インスタンス化させない。 */
    private TestCoreFileAdapter() {
    }

    /**
     * 本体ファイル器の中身を読み取り専用のスナップショットへ写す。
     *
     * @param file 本体ファイル器
     * @return ファイルビュー
     */
    public static FileView read(DataFile file) {
        List<FragmentView> fragments = new ArrayList<FragmentView>(file.all.size());
        for (DataFileFragment fragment : file.all) {
            fragments.add(new FragmentView(
                    copyOrNull(fragment.names),
                    copyOrNull(fragment.types),
                    copyOrNull(fragment.lengths),
                    copyValues(fragment.values)));
        }
        return new FileView(file.getPath(),
                new LinkedHashMap<String, Object>(file.directives),
                fragments);
    }

    /**
     * リストを防御的にコピーする（{@code null} はそのまま {@code null}）。
     *
     * @param list 対象（{@code null} 可）
     * @return コピー（{@code null} なら {@code null}）
     */
    private static List<String> copyOrNull(List<String> list) {
        return list == null ? null : new ArrayList<String>(list);
    }

    /**
     * データ行（フィールド名→値の Map のリスト）を防御的にコピーする。
     *
     * @param values 対象
     * @return コピー
     */
    private static List<Map<String, String>> copyValues(List<Map<String, String>> values) {
        List<Map<String, String>> copy = new ArrayList<Map<String, String>>(values.size());
        for (Map<String, String> row : values) {
            copy.add(new LinkedHashMap<String, String>(row));
        }
        return copy;
    }

    /**
     * 本体ファイル器の読み取り専用スナップショット。
     */
    public static final class FileView {

        /** ファイルパス */
        private final String path;

        /** ディレクティブ（型変換済み・記述順は保たない＝器固有挙動） */
        private final Map<String, Object> directives;

        /** 断片（レコードレイアウト単位） */
        private final List<FragmentView> fragments;

        /**
         * コンストラクタ。
         *
         * @param path       ファイルパス
         * @param directives ディレクティブ
         * @param fragments  断片
         */
        FileView(String path, Map<String, Object> directives, List<FragmentView> fragments) {
            this.path = path;
            this.directives = directives;
            this.fragments = fragments;
        }

        /** @return ファイルパス */
        public String getPath() {
            return path;
        }

        /** @return ディレクティブ（読み取り専用） */
        public Map<String, Object> getDirectives() {
            return Collections.unmodifiableMap(directives);
        }

        /** @return 断片一覧（読み取り専用） */
        public List<FragmentView> getFragments() {
            return Collections.unmodifiableList(fragments);
        }
    }

    /**
     * 本体ファイル断片（レコードレイアウト）の読み取り専用スナップショット。
     * <p>
     * レコード種別・長さ省略判定は本体で private のため保持しない。これらは呼び出し側が
     * 生行から復元する（設計書 §共通）。
     * </p>
     */
    public static final class FragmentView {

        /** フィールド名称 */
        private final List<String> names;

        /** データ型シンボル（器がフレームワーク表記へ正規化済み。{@code null} 可） */
        private final List<String> types;

        /** フィールド長（器が省略長を実バイト長へ上書き済み。可変長では {@code null}） */
        private final List<String> lengths;

        /** データ行（フィールド名→値） */
        private final List<Map<String, String>> values;

        /**
         * コンストラクタ。
         *
         * @param names   フィールド名称
         * @param types   データ型シンボル（{@code null} 可）
         * @param lengths フィールド長（{@code null} 可）
         * @param values  データ行
         */
        FragmentView(List<String> names, List<String> types, List<String> lengths,
                     List<Map<String, String>> values) {
            this.names = names;
            this.types = types;
            this.lengths = lengths;
            this.values = values;
        }

        /** @return フィールド名称（読み取り専用） */
        public List<String> getNames() {
            return Collections.unmodifiableList(names);
        }

        /** @return データ型シンボル（器が正規化済み。{@code null} 可） */
        public List<String> getTypes() {
            return types == null ? null : Collections.unmodifiableList(types);
        }

        /** @return フィールド長（器が省略長を上書き済み。{@code null} 可） */
        public List<String> getLengths() {
            return lengths == null ? null : Collections.unmodifiableList(lengths);
        }

        /** @return データ行（読み取り専用） */
        public List<Map<String, String>> getValues() {
            return Collections.unmodifiableList(values);
        }
    }
}
