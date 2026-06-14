package nablarch.test.core.reader.yaml;

import java.util.Collections;
import java.util.List;

import nablarch.test.core.util.interpreter.BinaryFileInterpreter;
import nablarch.test.core.util.interpreter.TestDataInterpreter;

/**
 * ビルダ（{@link YamlTableDataBuilder}／{@link YamlFileBuilder}／{@link YamlMessageBuilder}）が
 * 値加工（{@code interpret}）に用いるインタープリタチェーンを、取得元パス（basePath）ごとに解決する戦略。
 *
 * <p>
 * {@link BinaryFileInterpreter} は取得元パスに依存する（{@code ${binaryFile:相対パス}} を basePath
 * 起点で解決する）ため、ビルダ生成時には積めず読み込みごとに先頭へ積む必要がある。この「どのインタープリタを
 * 積むか」は<b>呼び出し側の責務</b>である（本体 {@code BasicTestDataParser} も get* ごとに動的に積む）。
 * ビルダ自身はインタープリタ方針を知らず、与えられたチェーンで値を加工するだけにする（責務分離）。
 * </p>
 *
 * <ul>
 * <li>実行時（{@link nablarch.test.core.reader.YamlTestDataParser}）は {@link #withBinaryFile} を用い、
 *     設定済みインタープリタの先頭に basePath 付き {@link BinaryFileInterpreter} を積む。</li>
 * <li>変換ツールの読み込み（{@link nablarch.test.core.reader.YamlTestCoreAdapter}）は {@link #raw} を用い、
 *     インタープリタを一切積まない＝IN 値を記法のまま（{@code ${binaryFile:...}} も未解決のまま）取り出す。</li>
 * </ul>
 *
 * @author kiyobot
 */
public interface InterpreterResolver {

    /**
     * 指定した取得元パスに対するインタープリタチェーンを返す。
     *
     * @param basePath 取得元パス（{@code ${binaryFile:...}} の相対解決起点）
     * @return インタープリタチェーン
     */
    List<TestDataInterpreter> resolve(String basePath);

    /**
     * 実行時用。設定済みインタープリタの先頭に basePath 付き {@link BinaryFileInterpreter} を積んで返す。
     *
     * @param base 設定済みインタープリタ（{@code null} 可）
     * @return basePath ごとに {@link BinaryFileInterpreter} を先頭へ積むリゾルバ
     */
    static InterpreterResolver withBinaryFile(List<TestDataInterpreter> base) {
        return basePath -> YamlSection.addBinaryFileInterpreter(basePath, base);
    }

    /**
     * 変換ツール（読み込み）用。インタープリタを一切積まず、値を記法のまま（未加工）扱う。
     *
     * @return 常に空のチェーンを返すリゾルバ
     */
    static InterpreterResolver raw() {
        return basePath -> Collections.<TestDataInterpreter>emptyList();
    }
}
