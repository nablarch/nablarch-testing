package nablarch.test;

import nablarch.core.util.Builder;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static nablarch.core.util.Builder.*;

/**
 * ユーティリティクラス。
 *
 * @author Tsuyoshi Kawasaki
 */
@Published(tag = "architect")
public final class NablarchTestUtils {

    /** カンマを示す正規表現 */
    private static final Pattern COMMA = Pattern.compile(",");

    /**
     * 指定された文字列をカンマ(,)で分割し、配列を生成する。<br>
     * 指定された文字列が、nullまたは空文字列の場合には、サイズ0の配列を返却する。
     *
     * @param str 文字列
     * @return 変換した配列
     */
    public static String[] makeArray(String str) {
        return (StringUtil.isNullOrEmpty(str))
                ? new String[0]
                : COMMA.split(str);
    }

    /**
     * LRUアルゴリズムのMap実装を生成する。
     *
     * @param maxSize Mapの最大サイズ
     * @param <K>     キーの型
     * @param <V>     値の型
     * @return LRUアルゴリズムのMap実装
     */
    public static <K, V> Map<K, V> createLRUMap(final int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException(Builder.concat(
                    "argument maxSize must not be less than zero. but was [", maxSize, "]"));
        }
        return new LinkedHashMap<K, V>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxSize;
            }
        };
    }

    /**
     * プライベートコンストラクタを起動
     *
     * @param target テスト対象クラス
     * @param <T>    テスト対象クラスの型
     */
    public static <T> void invokePrivateDefaultConstructor(Class<T> target) {
        if (target == null) {
            throw new IllegalArgumentException("argument must not be null.");
        }
        // プライベートデフォルトコンストラクタを取得
        Constructor<T> constructor = getPrivateDefaultConstructor(target);
        // コンストラクタ起動
        invoke(constructor);
    }

    /**
     * プライベートデフォルトコンストラクタを取得する
     *
     * @param target 取得対象クラス
     * @param <T>    取得対象クラスの型
     * @return コンストラクタ
     */
    private static <T> Constructor<T> getPrivateDefaultConstructor(Class<T> target) {
        Constructor<T> constructor = getDefaultConstructor(target);
        if (!Modifier.isPrivate(constructor.getModifiers())) {
            throw new IllegalArgumentException(
                    "constructor is NOT private scope. class=[" + target.getName() + "]");
        }
        return constructor;
    }


    /**
     * デフォルトコンストラクタを取得する
     *
     * @param target 取得対象クラス
     * @param <T>    取得対象クラスの型
     * @return コンストラクタ
     */
    private static <T> Constructor<T> getDefaultConstructor(Class<T> target) {
        try {
            return target.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "can't find default constructor. class=[" + target.getName() + "]", e);
        }
    }

    /**
     * コンストラクタを起動する。
     *
     * @param constructor 起動対象コンストラクタ
     */
    private static void invoke(Constructor<?> constructor) {
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("invoking constructor failed.", e);
        }
    }

    /**
     * リストの各要素を大文字に変換する。
     *
     * @param original 元のリスト
     * @return 変換後のリスト
     */
    public static List<String> toUpperCase(List<String> original) {
        if (original == null || original.isEmpty()) {
            return original;
        }
        List<String> uppers = new ArrayList<String>(original.size());
        for (String e : original) {
            uppers.add(e.toUpperCase());
        }
        return uppers;
    }

    /**
     * 拡張子を除いたファイル名を取得する。
     *
     * @param fileName 元のファイル名
     * @return 拡張子を除いたファイル名
     */
    public static String getFileNameWithoutSuffix(String fileName) {
        // null、空文字の場合そのまま返却
        if (StringUtil.isNullOrEmpty(fileName)) {
            return fileName;
        }

        int indexOfSuffix = fileName.indexOf(".");
        return (indexOfSuffix == -1)
                ? fileName
                : fileName.substring(0, indexOfSuffix);
    }

    /**
     * 配列をSetに変換する。
     *
     * @param array 変換対象の配列
     * @param <T>   配列の型
     * @return 変換後のSet
     */
    public static <T> Set<T> asSet(T... array) {
        return (array == null)
                ? null
                : new HashSet<T>(Arrays.asList(array));
    }


    /**
     * コレクションがnullまたは空であるか判定する。
     *
     * @param collection 判定対象
     * @return nullまたは空の場合、真
     */
    public static boolean isNullOrEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 正規化されたパスへ変換する。
     *
     * @param path 変換対象
     * @return 正規化されたパス
     */
    public static String toCanonicalPath(String path) {
        return toCanonical(new File(path)).getPath();
    }

    /**
     * 正規化されたファイルへ変換する。
     *
     * @param file 変換対象
     * @return 正規化されたファイル
     */
    public static File toCanonical(File file) {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null.");
        }
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * ファイルを削除する。<br/>
     * ディレクトリが指定された場合、そのディレクトリとその配下全てを削除する。
     * 指定されたファイルが存在しない場合は何もしない。
     *
     * @param target 削除対象ファイル
     */
    public static void deleteFile(File target) {
        if (target == null || !target.exists()) {
            return;
        }
        if (target.isDirectory()) {
            for (File child : target.listFiles()) {
                deleteFile(child);
            }
        }
        boolean success = target.delete();
        if (!success) {
            throw new IllegalStateException(
                    "can't delete " + target.getAbsolutePath());
        }
    }


    /**
     * リスト末尾の空要素（nullまたは空文字）を取り除く。（破壊的メソッド）
     *
     * @param list リスト
     * @return 引数のリスト自身
     */
    public static List<String> trimTail(List<String> list) {
        if (list == null) {
            return null;
        }
        // 末尾からループ
        for (int i = list.size() - 1; i >= 0; i--) {
            if (StringUtil.hasValue(list.get(i))) {
                break;  // 空でない要素があれば終了
            }
            list.remove(i); // 空要素を削除
        }
        return list;
    }



    /**
     * リスト末尾の空要素（nullまたは空文字）を取り除く。（非破壊的メソッド）
     *
     * @param orig 元となるリスト
     * @return 末尾の空要素が取り除かれた新しいリスト
     */
    public static List<String> trimTailCopy(List<String> orig) {
        if (orig == null) {
            return null;
        }
        List<String> copied = new ArrayList<String>(orig);
        return trimTail(copied);
    }

    /**
     * 文字列の長さを閾値まで制限する。<br/>
     * 文字列長が閾値を超えていた場合、先頭から閾値までの文字列を返却する。
     * そうでない場合は、元の文字列をそのまま返却する。
     *
     * @param string    対象文字列
     * @param threshold 閾値（0以上）
     * @return 短縮された文字列
     */
    public static String limit(String string, int threshold) {
        if (string == null) {
            throw new IllegalArgumentException("string must not be null.");
        }
        if (threshold < 0) {
            throw new IllegalArgumentException("threshold must not be negative. but was [" + threshold + "]");
        }
        // 閾値を超えていれば短縮する。
        return string.length() > threshold
                ? string.substring(0, threshold)
                : string;
    }


    /** 円マークのパターン */
    private static final Pattern YEN_PATTERN = Pattern.compile(Pattern.quote("\\"));
    /** エスケープ後の円マークのパターン */
    private static final Pattern ESCAPED_YEN_PATTERN = Pattern.compile(Pattern.quote("\\\\"));
    /** エスケープ後の円マーク */
    private static final String ESCAPED_YEN = Matcher.quoteReplacement("\\\\");
    /** エスケープ解除後の円マーク */
    private static final String UNESCAPED_YEN = Matcher.quoteReplacement("\\");

    /**
     * 文字列をエスケープする。
     *
     * @param orig エスケープ対象文字列
     * @return エスケープ後の文字列
     */
    public static String escape(String orig) {
        return YEN_PATTERN.matcher(orig).replaceAll(ESCAPED_YEN);  // \ -> \\
    }

    /**
     * 文字列のエスケープを解除する。
     *
     * @param escaped エスケープされた文字列
     * @return エスケープ解除された文字列
     */
    public static String unescapeYen(String escaped) {
        return ESCAPED_YEN_PATTERN.matcher(escaped).replaceAll(UNESCAPED_YEN);
    }

    /** プライベートコンストラクタ */
    private NablarchTestUtils() {
    }

    /**
     * リフレクションAPIを使用する際の簡易的なテンプレートクラス。<br/>
     * 例外が発生した場合の典型的な対処を提供する。
     *
     * @author T.Kawasaki
     */
    public abstract static class ReflectionOperation {
        /** 実行する。 */
        public final void execute() {
            try {
                operate();
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                throw new RuntimeException(cause);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * サブクラス、無名クラスでリフレクション操作を行う。
         *
         * @throws Exception 例外
         */
        protected abstract void operate() throws Exception;
    }

    /**
     * ファイルを出力ストリームとしてオープンする。<br/>
     *
     * @param outFilePath 出力先ファイルパス
     * @return 出力ストリーム
     */
    public static BufferedOutputStream openAsOutputStream(String outFilePath) {
        return openAsOutputStream(outFilePath, false);
    }

    /**
     * ファイルを出力ストリームとしてオープンする。<br/>
     *
     * @param outFilePath  出力先ファイルパス
     * @param deleteOnExit プログラム終了後にファイルを削除するか
     * @return 出力ストリーム
     */
    public static BufferedOutputStream openAsOutputStream(String outFilePath, boolean deleteOnExit) {
        File outFile = new File(outFilePath);

        touch(outFile);
        if (deleteOnExit) {
            outFile.deleteOnExit();
        }
        return openAsOutputStream(outFile);
    }

    /**
     * ファイルを出力ストリームとしてオープンする。<br/>
     *
     * @param outFile 出力先ファイル
     * @return 出力ストリーム
     */
    public static BufferedOutputStream openAsOutputStream(File outFile) {

        try {
            return new BufferedOutputStream(new FileOutputStream(outFile));
        } catch (IOException e) {
            throw new RuntimeException("can't open file. [" + outFile.getAbsolutePath() + "]", e);
        }
    }

    /**
     * ファイルを作成する。<br/>
     *
     * @param file 作成対象ファイル
     */
    public static void touch(File file) {
        if (file.exists()) {
            return;
        }
        try {
            boolean success = file.createNewFile();
            if (!success) {
                throw new RuntimeException("can't create file. [" + file + "]");
            }
        } catch (IOException e) {
            throw new RuntimeException("can't create file. [" + file + "]");
        }

    }

    /**
     * Mapに必須のキーが含まれていることを表明する。
     *
     * @param msgOnFail    表明失敗時のメッセージ
     * @param target       調査対象となるのMap
     * @param requiredKeys 必須のキー
     * @param <K>          キーの型
     */
    public static <K> void assertContainsRequiredKeys(String msgOnFail, Map<K, ?> target, Set<K> requiredKeys) {
        Set<K> required = new HashSet<K>(requiredKeys);
        Set<K> actual = target.keySet();
        required.removeAll(actual);
        if (required.isEmpty()) {
            return; // 必須カラムが全て存在する。
        }
        throw new IllegalArgumentException(concat(
                msgOnFail,
                " ;required column(s) not found ", required));
    }


    /**
     * Throwableサブクラスのメッセージを取得する。<br/>
     * ネストされた例外がある場合はそのメッセージも取得する。
     *
     * @param target メッセージ取得対象の{@link Throwable}
     * @return メッセージ
     */
    public static String getMessages(Throwable target) {
        if (target == null) {
            throw new IllegalArgumentException("argument must not be null.");
        }
        return getThrowableMessage(target, new ArrayList<String>());
    }

    /**
     * @param target      メッセージ取得対象の{@link Throwable}
     * @param accumulator 蓄積されたメッセージ
     * @return メッセージ
     * @see #getMessages(Throwable)
     */
    private static String getThrowableMessage(Throwable target, List<String> accumulator) {
        accumulator.add(target.getMessage());
        Throwable cause = target.getCause();
        boolean hasMoreCause = (cause != null && cause != target);
        return hasMoreCause
                ? getThrowableMessage(cause, accumulator)  // recursive call.
                : Builder.join(accumulator, " ; ");
    }

    /**
     * 文字列を整数値に変換する。
     *
     * @param intExpression 数字
     * @return 変換後の整数値
     * @throws IllegalArgumentException 文字列が整数値として解釈できない場合
     */
    public static int parseInt(String intExpression) throws IllegalArgumentException {
        try {
            return Integer.parseInt(intExpression);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(Builder.concat(
                    "argument must be numeric.",
                    " but was [", intExpression, "] "), e);
        }
    }

}
