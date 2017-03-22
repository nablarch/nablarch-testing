package nablarch.test.core.file;


import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.LayoutDefinition;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.test.RepositoryInitializer;
import nablarch.test.Trap;
import nablarch.test.support.tool.Hereis;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * {@link FixedLengthFile}のテストクラス。
 *
 * @author T.Kawasaki
 */
public class FixedLengthFileTest {

    /** コンポーネント設定ファイルに記載されたデフォルトのディレクティブが設定されていること。 */
    @Test
    public void testPrepareDefaultDirectives() {
        RepositoryInitializer.initializeDefaultRepository();
        FixedLengthFile target = new FixedLengthFile("hoge");
        Map<String, Object> directives = target.directives;

        assertEquals("Windows-31J", directives.get("text-encoding"));
        assertEquals("", directives.get("record-separator"));
    }


    /** 異なるレコードが設定された場合に例外が発生すること。 */
    @Test
    public void testRecordLengthDiffers() {
        final DataFile target = new FixedLengthFile("hoge");
        target.setDirective("text-encoding", "UTF-8");
        DataFileFragment one = target.getNewFragment();
        one.setNames(Arrays.asList("hoge"));
        one.setLengths(Arrays.asList("3"));
        one.setTypes(Arrays.asList("半角数字"));

        DataFileFragment another = target.getNewFragment();
        another.setNames(Arrays.asList("hoge", "fuga"));
        another.setLengths(Arrays.asList("3", "2"));
        another.setTypes(Arrays.asList("半角数字", "半角英字"));

        new Trap() {
            @Override
            protected void shouldFail() throws Exception {
                target.createLayout();
            }
        }.capture(IllegalStateException.class)
         .whichMessageContains("record-length differs",
                               "lengths=[3, 2]");
    }

    /**
     * データ変換が行われること
     */
    @Test
    public void testConvertDataExistsConverter() {
        // テスト用のリポジトリ構築
        File diConfigFile = Hereis.file("tmp/FixedLengthFileTest.xml");
        /*****
        <?xml version="1.0" encoding="UTF-8"?>
        <component-configuration
            xmlns="http://tis.co.jp/nablarch/component-configuration"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://tis.co.jp/nablarch/component-configuration  ./../component-configuration.xsd">
        
            <!-- データを一律HOGEに置き換えるテスト用コンバータ -->
            <component name="TestDataConverter_Hoge" 
                       class="nablarch.test.core.file.FixedLengthFileTest$HogeDataConvertor"/>
                                            
        </component-configuration>
        */
        diConfigFile.deleteOnExit();
        SystemRepository.clear();
        SystemRepository.load(new DiContainer(new XmlComponentDefinitionLoader(diConfigFile.toURI().toString())));

        FixedLengthFile file = new FixedLengthFile("dummyPath");
        file.directives.put("file-type", "Hoge");
        DataRecord targetData = new DataRecord();
        targetData.put("key1", "value1");
        targetData.put("key2", "value2");
        targetData.put("key3", "value3");
        
        DataRecord expectedData = new DataRecord();
        expectedData.put("key1", "HOGE");
        expectedData.put("key2", "HOGE");
        expectedData.put("key3", "HOGE");
        
        DataRecord actualData = file.convertData(new LayoutDefinition(), targetData);
        
        assertEquals(expectedData, actualData);
        
        SystemRepository.clear();
    }
    
    /**
     * データ変換が行われないこと
     */
    @Test
    public void testConvertDataNotExistsConverter() {
        // テスト用のリポジトリ構築
        File diConfigFile = Hereis.file("tmp/FixedLengthFileTest.xml");
        /*****
        <?xml version="1.0" encoding="UTF-8"?>
        <component-configuration
            xmlns="http://tis.co.jp/nablarch/component-configuration"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://tis.co.jp/nablarch/component-configuration  ./../component-configuration.xsd">
        
            <!-- データを一律HOGEに置き換えるテスト用コンバータ -->
            <component name="TestDataConverter_Hoge" 
                       class="nablarch.test.core.file.FixedLengthFileTest$HogeDataConvertor"/>
                                            
        </component-configuration>
         */
        diConfigFile.deleteOnExit();
        SystemRepository.clear();
        SystemRepository.load(new DiContainer(new XmlComponentDefinitionLoader(diConfigFile.toURI().toString())));
        
        FixedLengthFile file = new FixedLengthFile("dummyPath");
        file.directives.put("file-type", "Fixed"); // Fixedは未定義なのでデータがそのまま返却される
        DataRecord targetData = new DataRecord();
        targetData.put("key1", "value1");
        targetData.put("key2", "value2");
        targetData.put("key3", "value3");
        
        DataRecord expectedData = new DataRecord();
        expectedData.put("key1", "value1");
        expectedData.put("key2", "value2");
        expectedData.put("key3", "value3");
        
        DataRecord actualData = file.convertData(new LayoutDefinition(), targetData);
        
        assertEquals(expectedData, actualData);
        
        SystemRepository.clear();
    }
    
    /**
     * フォーマット定義が作成されること
     */
    @Test
    public void testCreateDefinitionExistsConverter() {
        // テスト用のリポジトリ構築
        File diConfigFile = Hereis.file("tmp/FixedLengthFileTest.xml");
        /*****
        <?xml version="1.0" encoding="UTF-8"?>
        <component-configuration
            xmlns="http://tis.co.jp/nablarch/component-configuration"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://tis.co.jp/nablarch/component-configuration  ./../component-configuration.xsd">
        
            <!-- 毎回新規フォーマット定義を作成するテスト用コンバータ -->
            <component name="TestDataConverter_Hoge" 
                       class="nablarch.test.core.file.FixedLengthFileTest$HogeDataConvertor"/>
                                            
        </component-configuration>
        */
        diConfigFile.deleteOnExit();
        SystemRepository.clear();
        SystemRepository.load(new DiContainer(new XmlComponentDefinitionLoader(diConfigFile.toURI().toString())));

        FixedLengthFile file = new FixedLengthFile("dummyPath");
        file.directives.put("file-type", "Hoge");
        
        LayoutDefinition originalLd = new LayoutDefinition();
        LayoutDefinition actualLd = file.createDefinition(originalLd, new DataRecord());
        
        Assert.assertNotSame(originalLd, actualLd);
        
        SystemRepository.clear();
    }
    
    /**
     * フォーマット定義が作成されないこと
     */
    @Test
    public void testCreateDefinitionNotExistsConverter() {
        // テスト用のリポジトリ構築
        File diConfigFile = Hereis.file("tmp/FixedLengthFileTest.xml");
        /*****
        <?xml version="1.0" encoding="UTF-8"?>
        <component-configuration
            xmlns="http://tis.co.jp/nablarch/component-configuration"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://tis.co.jp/nablarch/component-configuration  ./../component-configuration.xsd">
        
            <!-- データを一律HOGEに置き換えるテスト用コンバータ -->
            <component name="TestDataConverter_Hoge" 
                       class="nablarch.test.core.file.FixedLengthFileTest$HogeDataConvertor"/>
                                            
        </component-configuration>
         */
        diConfigFile.deleteOnExit();
        SystemRepository.clear();
        SystemRepository.load(new DiContainer(new XmlComponentDefinitionLoader(diConfigFile.toURI().toString())));
        
        FixedLengthFile file = new FixedLengthFile("dummyPath");
        file.directives.put("file-type", "Fixed"); // Fixedは未定義なのでフォーマット定義がそのまま返却される
        
        LayoutDefinition originalLd = new LayoutDefinition();
        LayoutDefinition actualLd = file.createDefinition(originalLd, new DataRecord());
        
        Assert.assertSame(originalLd, actualLd);
        
        SystemRepository.clear();
    }
    

    public static class HogeDataConvertor implements TestDataConverter {
        @Override
        public DataRecord convertData(LayoutDefinition definition,
                DataRecord currentData, Charset encoding) {
            for (String key : currentData.keySet()) {
                currentData.put(key, "HOGE");
            }
            return currentData;
        }
        
        @Override
        public LayoutDefinition createDefinition(
                LayoutDefinition defaultDefinition, DataRecord currentData,
                Charset encoding) {
            return new LayoutDefinition();
        }
    }
}
