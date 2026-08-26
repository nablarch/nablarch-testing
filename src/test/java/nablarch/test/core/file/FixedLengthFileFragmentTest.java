package nablarch.test.core.file;


import nablarch.core.dataformat.DataRecordFormatterSupport.Directive;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.test.RepositoryInitializer;
import nablarch.test.Trap;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * {@link DataFileFragment}のテストクラス。
 *
 * @author T.Kawasaki
 */
public class FixedLengthFileFragmentTest {

    /**システムリポジトリを元に戻す。*/
    @After
    public void tearDown(){
        SystemRepository.clear();
        RepositoryInitializer.initializeDefaultRepository();
    }

    /** nullが許容されないこと */
    @Test(expected = IllegalArgumentException.class)
    public void testSetNamesNull() {
        FixedLengthFile container = new FixedLengthFile("path/to/file");
        container.setDirective(Directive.TEXT_ENCODING.getName(), "utf-8");
        FixedLengthFileFragment target = new FixedLengthFileFragment(container);
        target.setNames(null);
    }

    /** 空のリストが許容されないこと */
    @Test(expected = IllegalArgumentException.class)
    public void testSetNamesEmpty() {
        FixedLengthFile container = new FixedLengthFile("path/to/file");
        container.setDirective(Directive.TEXT_ENCODING.getName(), "utf-8");
        FixedLengthFileFragment target = new FixedLengthFileFragment(container);
        target.setNames(Collections.<String>emptyList());
    }

    /** nullが許容されないこと */
    @Test(expected = IllegalArgumentException.class)
    public void testSetLengthsNull() {
        FixedLengthFile container = new FixedLengthFile("path/to/file");
        container.setDirective(Directive.TEXT_ENCODING.getName(), "utf-8");
        FixedLengthFileFragment target = new FixedLengthFileFragment(container);
        target.setLengths(null);
    }

    /** 空のリストが許容されないこと */
    @Test(expected = IllegalArgumentException.class)
    public void testSetLengthsEmpty() {
        FixedLengthFile container = new FixedLengthFile("path/to/file");
        container.setDirective(Directive.TEXT_ENCODING.getName(), "utf-8");
        FixedLengthFileFragment target = new FixedLengthFileFragment(container);
        target.setLengths(Collections.<String>emptyList());
    }

    /** nullが許容されないこと */
    @Test(expected = IllegalArgumentException.class)
    public void testSetTypesNull() {
        FixedLengthFile container = new FixedLengthFile("path/to/file");
        container.setDirective(Directive.TEXT_ENCODING.getName(), "utf-8");
        FixedLengthFileFragment target = new FixedLengthFileFragment(container);
        target.setTypes(null);
    }

    /** 空のリストが許容されないこと */
    @Test(expected = IllegalArgumentException.class)
    public void testSetTypesEmpty() {
        FixedLengthFile container = new FixedLengthFile("path/to/file");
        container.setDirective(Directive.TEXT_ENCODING.getName(), "utf-8");
        FixedLengthFileFragment target = new FixedLengthFileFragment(container);
        target.setTypes(Collections.<String>emptyList());
    }

    /** 「-」が設定されているレコードが存在する場合に、値の追加時に、サイズの自動計算が行われること。*/
    @Test
    public void testAutoCalcRecordLengthWhenAddValue() {
        final DataFile target = new FixedLengthFile("hoge");
        target.setDirective("text-encoding", "UTF-8");
        DataFileFragment one = target.getNewFragment();
        one.setNames(Arrays.asList("hoge", "foo"));
        one.setLengths(Arrays.asList("4","-"));
        one.setTypes(Arrays.asList("半角英字","半角英字"));
        //値の追加
        one.addValue(Arrays.asList("123", "12345"));
        assertThat(one.getLengthOf("foo"), is(5));
    }
    
    /** 「-」が設定されているレコードが存在する場合に、値の追加(ID有り)時に、サイズの自動計算が行われること。*/
    @Test
    public void testAutoCalcRecordLengthaddValueWithId() {
        final DataFile target = new FixedLengthFile("hoge");
        target.setDirective("text-encoding", "UTF-8");
        DataFileFragment one = target.getNewFragment();
        one.setNames(Arrays.asList("hoge", "foo"));
        one.setLengths(Arrays.asList("4", "-"));
        one.setTypes(Arrays.asList("半角英字","半角英字"));
        //値の追加(ID有り)
        one.addValueWithId(Arrays.asList("123","12345"), "1");
        assertThat(one.getLengthOf("foo"), is(5));
    }

    /**
     * 文字コード別マッピングが定義されている場合は、文字コード別マッピングが使われること
     */
    @Test
    public void testSetTypesMatchEncodingDef() {
        SystemRepository.clear();
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/test/core/file/encodingMappingWithDefault_BasicDataTypeMapping.xml");
        DiContainer diContainer = new DiContainer(loader);
        SystemRepository.load(diContainer);

        FixedLengthFile container = new FixedLengthFile("path/to/file");

        //マッピングの定義が存在する文字コードを指定
        container.setDirective(Directive.TEXT_ENCODING.getName(), "utf-8");

        FixedLengthFileFragment target = new FixedLengthFileFragment(container);
        List<String> nameList = new ArrayList<String>();
        nameList.add("氏名");
        target.setNames(nameList);
        List<String> typeList = new ArrayList<String>();
        typeList.add("全角");
        target.setTypes(typeList);
        
        //コンポーネント名「dataTypeMapping_UTF-8」にて定義されたマッピングと一致
        assertThat(target.types.get(0), is("XN"));
    }

    /**
     * 文字コード別マッピングが定義されていない場合に、コンポーネント名dataTypeMappingの定義が存在すれば、その定義が使われること
     */
    @Test
    public void testSetTypesNoMatchEncodingDefWithDefault() {
        SystemRepository.clear();
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/test/core/file/encodingMappingWithDefault_BasicDataTypeMapping.xml");
        DiContainer diContainer = new DiContainer(loader);
        SystemRepository.load(diContainer);

        FixedLengthFile container = new FixedLengthFile("path/to/file");

        //マッピングの定義が存在しない文字コードを指定
        container.setDirective(Directive.TEXT_ENCODING.getName(), "Shift_JIS");

        FixedLengthFileFragment target = new FixedLengthFileFragment(container);
        List<String> nameList = new ArrayList<String>();
        nameList.add("氏名");
        target.setNames(nameList);
        List<String> typeList = new ArrayList<String>();
        typeList.add("全角");
        target.setTypes(typeList);
        
        //コンポーネント名「dataTypeMapping」にて定義されたマッピングと一致
        assertThat(target.types.get(0), is("B"));
    }

    /**
     * 文字コード別マッピングも、コンポーネント名dataTypeMappingのマッピングも存在しなければ、組み込みのマッピングが使われること
     */
    @Test
    public void testSetTypesNoMatchEncodingDef() {
        SystemRepository.clear();
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/test/core/file/encodingMapping_BasicDataTypeMapping.xml");
        DiContainer diContainer = new DiContainer(loader);
        SystemRepository.load(diContainer);

        FixedLengthFile container = new FixedLengthFile("path/to/file");

        //マッピングの定義が存在しない文字コードを指定
        container.setDirective(Directive.TEXT_ENCODING.getName(), "Shift_JIS");

        FixedLengthFileFragment target = new FixedLengthFileFragment(container);
        List<String> nameList = new ArrayList<String>();
        nameList.add("氏名");
        target.setNames(nameList);
        List<String> typeList = new ArrayList<String>();
        typeList.add("全角");
        target.setTypes(typeList);
        
        //組み込みのマッピングとと一致
        assertThat(target.types.get(0), is("N"));
    }

    /**
     * 文字コードが設定されていない場合は、コンポーネント名dataTypeMappingのマッピングがあればそれが使用されること
     */
    @Test
    public void testSetTypesNoDefEncoding() {
        SystemRepository.clear();
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/test/core/file/encodingMappingWithDefault_BasicDataTypeMapping.xml");
        DiContainer diContainer = new DiContainer(loader);
        SystemRepository.load(diContainer);

        FixedLengthFile container = new FixedLengthFile("path/to/file");

        FixedLengthFileFragment target = new FixedLengthFileFragment(container);
        List<String> nameList = new ArrayList<String>();
        nameList.add("氏名");
        target.setNames(nameList);
        List<String> typeList = new ArrayList<String>();
        typeList.add("全角");
        target.setTypes(typeList);
        
        //コンポーネント名「dataTypeMapping」にて定義されたマッピングと一致
        assertThat(target.types.get(0), is("B"));
    }

    /** フィールド長が合算されること */
    @Test
    public void testCalcRecordLength() {
        FixedLengthFile container = new FixedLengthFile("path/to/file");
        container.setDirective(Directive.TEXT_ENCODING.getName(), "utf-8");
        FixedLengthFileFragment target = new FixedLengthFileFragment(container);
        assertThat(target.calcRecordLength(asList("1", "2", "3")), is(6));
    }

    /** フィールド長の合算に失敗した場合、例外が発生すること */
    @Test(expected = IllegalArgumentException.class)
    public void testCalcRecordLengthFail() {
        FixedLengthFile container = new FixedLengthFile("path/to/file");
        container.setDirective(Directive.TEXT_ENCODING.getName(), "utf-8");
        FixedLengthFileFragment target = new FixedLengthFileFragment(container);
        target.setNames(asList("foo", "bar", "buz"));
        target.calcRecordLength(asList("1", "2", "NaN"));
    }

    /** 重複した名前を設定した場合、例外が発生すること */
    @Test
    public void testSetDuplicateNames() {
        FixedLengthFile container = new FixedLengthFile("path/to/file");
        container.setDirective(Directive.TEXT_ENCODING.getName(), "utf-8");
        final FixedLengthFileFragment target = new FixedLengthFileFragment(container);
        new Trap("重複した名前が許容されないこと。") {
            @Override
            protected void shouldFail() throws Exception {
                target.setNames(asList("foo", "bar", "buz", "foo", "buz", "foo"));
            }
        }.capture(IllegalArgumentException.class)
         .whichMessageContains("Duplicate field names are not permitted in a record",
                               "[foo, buz]",
                               "path/to/file");

    }

    @Test
    public void testConvertBytesFillZeros() {
        FixedLengthFile container = new FixedLengthFile("path/to/file");

        FixedLengthFileFragment target = new FixedLengthFileFragment(container);
        target.setNames(asList("aaa"));
        target.setTypes(asList("バイナリ"));
        target.setLengths(asList("2"));
        target.getRecordDefinition();
        byte[] bytes = (byte[]) target.convertValue("aaa", "0x30");

        assertThat("0埋めされること",
                bytes, is(new byte[] {0x30, 0x00}));
    }

    /** 桁あふれが発生した場合、例外がスローされること。*/
    @Test(expected = IllegalStateException.class)
    public void testConvertBytesFail() {
        FixedLengthFile container = new FixedLengthFile("path/to/file");
        FixedLengthFileFragment target = new FixedLengthFileFragment(container);
        target.setNames(asList("aaa"));
        target.setTypes(asList("バイナリ"));
        target.setLengths(asList("2"));
        target.getRecordDefinition();
        target.convertValue("aaa", "0x303132"); // あふれ
    }
}
