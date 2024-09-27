package nablarch.test.core.file;

import java.nio.charset.Charset;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.LayoutDefinition;
import nablarch.core.util.annotation.Published;

/**
 * テストデータコンバータ<br>
 * <p>
 * 本インタフェースを実装することにより、エクセルファイルに記述されたテストデータに対し、任意の変換処理を行うことが可能です。<br>
 * 
 * @author TIS
 */
@Published
public interface TestDataConverter {
    
    /**
     * 現在処理中のテストデータに対応したレイアウト定義データを生成します。
     * 
     * @param defaultDefinition エクセルファイルに記述されたデータから生成されたデフォルトのレイアウト定義データ
     * @param currentData 現在処理中の{@link #convertData(LayoutDefinition, DataRecord, Charset)} )} 呼出し後のテストデータ。
     * @param encoding ディレクティブより取得したエンコーディング
     * @return 現在処理中のテストデータに対応したレイアウト定義データ
     */
    LayoutDefinition createDefinition(LayoutDefinition defaultDefinition, DataRecord currentData, Charset encoding);
    
    /**
     * テストデータを変換します。
     * 
     * @param definition エクセルファイルに記述されたデータから生成されたデフォルトのレイアウト定義データ
     * @param currentData 現在処理中のエクセルファイルに記述されたテストデータ。
     * @param encoding ディレクティブより取得したエンコーディング
     * @return 任意の変換処理を行ったテストデータ
     */
    DataRecord convertData(LayoutDefinition definition, DataRecord currentData, Charset encoding);
}
