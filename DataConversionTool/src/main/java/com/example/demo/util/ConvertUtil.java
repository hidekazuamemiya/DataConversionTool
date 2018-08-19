package com.example.demo.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellAddress;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.constant.Constants;
import com.example.demo.constant.Settings;
import com.example.demo.exception.ApplicationException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

/**
 * データコンバート　ユーティリティ
 */

@Component
public class ConvertUtil {

	private Settings settings;

	@Autowired
	public void Settings(Settings settings) {
		this.settings = settings;
	}

	private static DataFormatter dataFormatter;
	private static FormulaEvaluator formulaEvaluator;
	private static boolean useCachedFormulaResult = true;

	/** GETメソッド  */
	private static final String GET = "GET";
	/** SETメソッド */
	private static final String SET = "SET";

	/**
	 * パッケージ配下のオブジェクトを生成します
	 *
	 * @return 生成されたオブジェクト
	 * @throws Exception
	 */
	public List<Object> generateInstance(File res, String packagePath) throws Exception {

		List<Object> instancesList = new ArrayList<Object>();

		// ファイル数分のファクトインスタンスを取得
		Map<String, List<List<String>>> valueSet = getValueInstance(res);
		if (valueSet == null) {
			String errMsg = "データが見つかりません。";
			throw new Exception(errMsg);
		}
		// ファクト毎の繰り返し処理
		for (Map.Entry<String, List<List<String>>> factEntry : valueSet.entrySet()) {

			Class<?> classParts = getClassForName(packagePath + Constants.Package.SEPARATOR + factEntry.getKey());

			// 行毎の繰り返し処理
			for (int rowNum = 1; rowNum < factEntry.getValue().size(); rowNum++) {

				Object instance = factInstance(classParts);
				BeanWrapper bwObj = PropertyAccessorFactory.forBeanPropertyAccess(instance);

				// 項目単位での繰り返し処理
				for (int colNum = 0; colNum < factEntry.getValue().get(rowNum).size(); colNum++) {
					String fieldName = factEntry.getValue().get(0).get(colNum);
					String value = factEntry.getValue().get(rowNum).get(colNum);

					bwObj.setPropertyValue(fieldName, value);
				}
				instancesList.add(instance);
			}

		}
		return instancesList;
	}

	/**
	 * 指定された文字列名を持つクラスまたはインタフェースに関連付けられた、Classオブジェクトを返す.
	 *
	 * @param className
	 *            クラス名
	 * @return Classオブジェクト.
	 */
	@SuppressWarnings("unchecked")
	public <T> Class<T> getClassForName(String className) {
		try {
			return (Class<T>) Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * ファクトのインスタンスを作成する.
	 *
	 * <p>
	 * ファクトの各String項目は nullで初期化する.
	 *
	 * @param factType
	 *            ファクトのタイプ
	 * @return 作成したインスタンス.
	 */
	public static <T> T factInstance(Class<T> factType) {
		T fact;
		try {
			fact = factType.getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new ApplicationException(factType.getName() + " のインスタンス作成に失敗しました。", e);
		}

		return fact;
	}

	/**
	 * パッケージ配下のリソースを取得します。
	 *
	 * @param returnKbn
	 * 0:クラスパス 1:クラス名のみ
	 * @param packagePath
	 * @return
	 */
	public List<String> getClassNameList(int returnKbn, String packagePath) {
		// クラスローダを利用して、パッケージ配下のリソースを取得する。
		String rootPackageName = packagePath.replace(Constants.Package.SEPARATOR, File.separator);
		ClassLoader classLoader = ClassLoader.getSystemClassLoader();
		Enumeration<URL> rootUrls = null;
		try {
			rootUrls = classLoader.getResources(rootPackageName);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			throw new ApplicationException("予期せぬエラーが発生しました。", e);
		}

		// ディレクトリを再帰的に探索して、".class"で終わるファイルを見つけた場合は
		// 文字列を整形したのちにリストへ格納しておく。
		List<String> classNames = new ArrayList<>();
		while (rootUrls.hasMoreElements()) {
			URL rootUrl = rootUrls.nextElement();
			Path rootPath = null;
			try {
				rootPath = Paths.get(rootUrl.toURI());
			} catch (URISyntaxException e) {
				System.err.println(e.getMessage());
				throw new ApplicationException("予期せぬエラーが発生しました。", e);
			}

			try {
				Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
						String pathName = path.toString();
						if (pathName.endsWith(Constants.Package.CLASS_SUFFIX)) {
							int beginIndex = pathName.lastIndexOf(rootPackageName);
							int endIndex = pathName.lastIndexOf(Constants.Package.CLASS_SUFFIX);
							String className = pathName.substring(beginIndex, endIndex).replace(File.separator,
									Constants.Package.SEPARATOR);
							String cName = className.substring(className.lastIndexOf(Constants.Package.SEPARATOR) + 1);

							if (returnKbn == 0) {
								// クラスパス
								classNames.add(className);
							} else {
								// クラス名のみ
								classNames.add(cName);
							}
						}

						return super.visitFile(path, attrs);
					}
				});
			} catch (IOException e) {
				System.err.println(e.getMessage());
				throw new ApplicationException("予期せぬエラーが発生しました。", e);
			}
		}
		return classNames;
	}

	/**
	 * クラスのメンバーを取得します。
	 *
	 * @param returnKbn
	 * 0:クラス名.メンバー名 1:メンバー名のみ 2:メンバー名,型
	 * @param packagePath
	 * @return
	 */
	public List<String> getClassMemberNameList(int returnKbn, String className) {
		List<String> classMemberNames = new ArrayList<>();
		String cName = className.substring(className.lastIndexOf(Constants.Package.SEPARATOR) + 1);
		// クラス名からの Class オブジェクトの取得
		Class<?> c = null;
		try {
			c = Class.forName(className);
		} catch (ClassNotFoundException e) {
			System.err.println(e.getMessage());
			throw new ApplicationException("予期せぬエラーが発生しました。", e);
		}
		// 0:クラス名.メンバー名 1:メンバー名のみ 2:メンバー名,型
		for (Field field : c.getDeclaredFields()) {
			if (returnKbn == 0) {
				classMemberNames.add(cName + Constants.Package.SEPARATOR + field.getName());
			} else if (returnKbn == 1) {
				classMemberNames.add(field.getName());
			} else {
				String fieldName = field.getName();
				String typeName = field.getType().getName();
				classMemberNames.add(fieldName + Constants.Character.COMMA + typeName);
			}
		}
		return classMemberNames;
	}

	/**
	 * クラスのメッソッド一覧を取得します。
	 *
	 * @param packagePath
	 * @return
	 */
	public List<String> getClassMethodNameList(String className) {
		try {
			Class<?> clazz = Class.forName(className);
			Method[] methods = clazz.getDeclaredMethods();

			List<String> classMethodNames = new ArrayList<>();
			for (Method method : methods) {
				String methodName = method.getName();
				classMethodNames.add(methodName);
			}
			return classMethodNames;
		} catch (Exception e) {
			System.err.println(e.getMessage());
			throw new ApplicationException("予期せぬエラーが発生しました。", e);
		}
	}

	// ----------------------------------------------------------『 newInstance 』
	/**
	 * 文字列「className」からインスタンスを生成し返します。
	 * @param className 完全修飾クラス名
	 * @return 完全修飾クラス名の新しいインスタンス
	 * @throws Exception
	 */
	public Object newInstance(String className) throws Exception {
		try {
			return Class.forName(className).newInstance();
		} catch (NoClassDefFoundError e) {
			System.err.println("NoClassDefFoundError : " + className);
			throw e;
		}
	}

	/**
	 * 文字列「className」からインスタンスを生成し返します。
	 * @param className 完全修飾クラス名
	 * @param argObj コンストラクタの引数
	 * @return 完全修飾クラス名の新しいインスタンス
	 * @throws Exception
	 */
	public Object newInstance(String className, Object[] argObj)
			throws Exception {
		Class[] argClass = new Class[argObj.length];
		for (int i = 0; i < argObj.length; i++) {
			argClass[i] = argObj[i].getClass();
		}
		Constructor c = Class.forName(className).getConstructor(argClass);
		return c.newInstance(argObj);
	}

	/**
	 * クラス「clazz」からインスタンスを生成し返します。
	 * @param clazz クラス
	 * @return clazzの新しいインスタンス
	 * @throws Exception
	 */
	public Object newInstance(Class clazz) throws Exception {
		return clazz.newInstance();
	}

	/**
	 * クラス「clazz」からインスタンスを生成し返します。
	 * @param clazz クラス
	 * @param argObj コンストラクタの引数
	 * @return clazzの新しいインスタンス
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public Object newInstance(Class clazz, Object[] argObj)
			throws Exception {
		Class[] argClass = new Class[argObj.length];
		for (int i = 0; i < argObj.length; i++) {
			argClass[i] = argObj[i].getClass();
		}
		Constructor c = clazz.getConstructor(argClass);
		return c.newInstance(argObj);
	}

	// ---------------------------------------------------------------『 Method 』
	/**
	 * オブジェクト「invokeObject」のフィールド「fieldName」のsetterメソッドを 呼び出し、値「value」を格納します。
	 *
	 * setterメソッドがなければフィールドへダイレクトに値を設定します。 ただし、この場合対象プロパティのアクセス修飾子はpublicであること
	 * @param invokeObject 実行対象のオブジェクト
	 * @param fieldName 実行対象のオブジェクトのプロパティ名
	 * @param value セットする値
	 * @throws Exception 以下の例外が発生します。
	 * @throws InvocationTargetException 基本となるメソッドが例外をスローする場合
	 * @throws IllegalAccessException この Method オブジェクトが Java
	 *             言語アクセス制御を実施し、基本となるメソッドにアクセスでき ない場合
	 * @throws NoSuchMethodException 指定された名前のメソッドが見つからない場合
	 */
	public void setProperty(Object invokeObject, String fieldName, Object value) throws Exception {
		try {
			Method method = searchMethod(invokeObject, fieldName, SET);
			Class[] paramClasses = method.getParameterTypes();
			Object[] valueArray = null;
			if (paramClasses[0].isInstance(value)) {
				// セットするオブジェクトが引数のクラスのサブクラスなら変換しない。
				valueArray = new Object[] { value };
			} else {
				valueArray = new Object[] { convObject(value, paramClasses[0].getName()) };
			}
			method.invoke(invokeObject, valueArray);
			//TODO 確認用
			//method = searchMethod(invokeObject, fieldName, GET);
			//System.out.println(fieldName + " : " + method.invoke(invokeObject));
		} catch (NoSuchMethodException e) {
			try {
				// setterメソッドがなければフィールドにダイレクトにセットする。
				setField(invokeObject, fieldName, value);
			} catch (NoSuchFieldException fe) {
				String errorMes = "\nクラス" + getShortClassName(invokeObject)
						+ "は、" + "フィールド「" + fieldName + "」に対し\n"
						+ "アクセス可能なセッターメソッドがなく、かつ。" + "フィールド「" + fieldName
						+ "」もpublicではありません。" + "";
				throw new IllegalAccessException(errorMes);
			}
		}
	}

	/**
	 * オブジェクト invokeObject のフィールド fieldName のgetterメソッドを 呼び出し値を取得します。
	 * getterメソッドがなければフィールドからダイレクトに値を取得します。 ただし、この場合対象プロパティのアクセス修飾子はpublicであること
	 * @param invokeObject 実行対象のオブジェクト
	 * @param fieldName 実行対象のオブジェクトのプロパティ名
	 * @return ゲッターメソッドのリターン値
	 * @throws Exception 以下の例外が発生します。
	 * @throws InvocationTargetException 基本となるメソッドが例外をスローする場合
	 * @throws IllegalAccessException この Method オブジェクトが Java
	 *             言語アクセス制御を実施し、基本となるメソッドにアクセスでき ない場合
	 * @throws NoSuchFieldException 指定された名前のフィールドが見つからない場合
	 */
	public Object getProperty(Object invokeObject, String fieldName)
			throws Exception {
		try {
			Method method = searchMethod(invokeObject, fieldName, GET);
			return method.invoke(invokeObject);
		} catch (NoSuchMethodException e) {
			return getField(invokeObject, fieldName);
		}
	}

	/**
	 * オブジェクト「invokeObject」のメソッド「callMethod」を実行します。
	 * リターン値がある場合は、Object形として得る事ができます。
	 * @param invokeObject 実行対象のオブジェクト
	 * @param callMethod 実行対象のメソッド名
	 * @param argObjects 引数がある場合はオブジェクトの配列として渡す。 引数が無い場合はnullを渡します。
	 * @return 「callMethod」を実行したリターン値
	 * @throws InvocationTargetException 基本となるメソッドが例外をスローする場合
	 * @throws IllegalAccessException この Method オブジェクトが Java
	 *             言語アクセス制御を実施し、基本となるメソッドにアクセスでき ない場合
	 * @throws NoSuchMethodException 指定された名前のメソッドが見つからない場合
	 */
	public Object invoke(Object invokeObject, String callMethod, Object[] argObjects)
			throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		Method method = findMethod(invokeObject, callMethod, argObjects);
		return method.invoke(invokeObject, argObjects);
	}

	/**
	 * オブジェクト「invokeObject」のメソッド「callMethod」を検索します。
	 * @param invokeObject 実行対象のオブジェクト
	 * @param callMethod 実行対象のオブジェクトのメソッド名
	 * @param argObjects 引数がある場合はオブジェクトの配列として渡す。 引数が無い場合はnullを渡します。
	 * @return 指定された引数の条件に一致するMethod オブジェクト
	 * @throws NoSuchMethodException 一致するメソッドが見つからない場合、 あるいは名前が "" または ""の場合
	 */
	public Method findMethod(Object invokeObject, String callMethod,
			Object[] argObjects) throws NoSuchMethodException {
		Class[] paramClasses = null;
		Method[] methods = invokeObject.getClass().getMethods();
		top: for (int i = 0; i < methods.length; i++) {
			if (methods[i].getName().equals(callMethod)) {
				if (argObjects == null
						&& methods[i].getParameterTypes().length == 0) {
					return methods[i];
				}
				if (argObjects == null) {
					continue;
				}
				paramClasses = methods[i].getParameterTypes();
				if (paramClasses.length == argObjects.length) {
					// 全てのパラメーターリストの型と、引数の型の検証
					for (int j = 0; j < paramClasses.length; j++) {
						Class paramClass = paramClasses[j];
						Object argObj = argObjects[j];
						// 引数の型がプリミティブの場合、引数のオブジェクト
						// がnullでなくプリミティブ
						// もしくわ、NumberのサブクラスならＯＫとする。
						if (argObj == null) {
							continue;
						}
						if (paramClass.isPrimitive()
								&& (argObj instanceof Number || argObj
										.getClass().isPrimitive())) {
							continue;
						}
						if (!paramClass.isInstance(argObj)) {
							// 型に暗黙変換の互換性が無い時点で、次のメソッドへ
							continue top;
						}
					}
					return methods[i];
				}
			}
		}
		String paramLength = (paramClasses != null) ? Integer
				.toString(paramClasses.length) : "";
		String errorMes = getShortClassName(invokeObject) + "にメソッド"
				+ callMethod + "はありません。" + "[ paramClasses.length ] = "
				+ paramLength + ",[ argObjects.length ] = " + argObjects.length
				+ "";
		throw new NoSuchMethodException(errorMes);
	}

	// ----------------------------------------------------------------『 Field 』
	/**
	 * 実行対象のオブジェクト「invokeObject」のフィールド名「fieldName」に値 「value 」を格納します。
	 * @param invokeObject 実行対象のオブジェクト
	 * @param fieldName 実行対象のオブジェクトのフィールド名
	 * @param value セットする値
	 * @throws IllegalAccessException 指定されたオブジェクトが基本と なるフィールド (またはそのサブクラスか実装側)
	 *             を宣言する クラスまたはインタフェースのインスタンスではない場合、 ある いはラップ解除変換が失敗した場合
	 * @throws NoSuchFieldException 指定された名前のフィールドが見つからない場合
	 */
	public static void setField(Object invokeObject, String fieldName,
			Object value) throws IllegalAccessException, NoSuchFieldException {
		Field field = searchField(invokeObject, fieldName);
		String className = field.getType().getName();
		Object convObj = null;
		if (field.getType().isInstance(value)) {
			convObj = value;
		} else {
			convObj = convObject(value, className);
		}
		field.set(invokeObject, convObj);
	}

	/**
	 * 実行対象のオブジェクト「invokeObject」のフィールド名「fieldName」の値を 取得します。
	 * @param invokeObject 実行対象のオブジェクト
	 * @param fieldName 実行対象のオブジェクトのフィールド名
	 * @return リターン値
	 * @throws IllegalAccessException 指定されたオブジェクトが基本と なるフィールド (またはそのサブクラスか実装側)
	 *             を宣言する クラスまたはインタフェースのインスタンスではない場合、 ある いはラップ解除変換が失敗した場合
	 * @throws NoSuchFieldException 指定された名前のフィールドが見つからない場合
	 */
	public Object getField(Object invokeObject, String fieldName)
			throws IllegalAccessException, NoSuchFieldException {
		Field field = searchField(invokeObject, fieldName);
		return field.get(invokeObject);
	}

	/**
	 * オブジェクト「object」がフィールド名「fieldName」を 宣言しているかどうかを 確認します。
	 * @param object 検査対象のオブジェクト
	 * @param fieldName 検査するフィールド名
	 * @return 宣言している場合true
	 * @throws Exception
	 */
	public boolean hasField(Object object, String fieldName)
			throws Exception {
		PropertyDescriptor[] props = getPropertyDescriptors(object);
		for (int i = 0; i < props.length; i++) {
			String _fieldName = props[i].getName();
			if (fieldName.equals(_fieldName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 *
	 * @param object
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public TreeSet getAllFields(Object object) throws Exception {

		TreeSet fieldSet = new TreeSet();
		// メソッドからプロパ－ティ名の取得
		PropertyDescriptor[] props = getPropertyDescriptors(object);
		for (int i = 0; i < props.length; i++) {
			String fieldName = props[i].getName();
			fieldSet.add(fieldName);

		}

		// フィールドからプロパ－ティ名の取得
		Field[] fields = object.getClass().getFields();
		for (int i = 0; i < fields.length; i++) {
			String fieldName = fields[i].getName();
			if (!fieldSet.contains(fieldName)) {
				fieldSet.add(fieldName);
			}
		}
		return fieldSet;
	}

	/**
	 *
	 * @param invokeObject 実行対象のオブジェクト
	 * @param fieldName 実行対象のオブジェクトのフィールド名
	 * @return 指定された引数の条件に一致する Filed オブジェクト
	 * @throws NoSuchFieldException 指定された名前のフィールドが見つからない場合
	 */
	private static Field searchField(Object invokeObject, String fieldName)
			throws NoSuchFieldException {
		try {
			return invokeObject.getClass().getField(fieldName);
		} catch (NoSuchFieldException e) {
			// このスコープはテーブルカラム名からの取得
			fieldName = checkFieldName(fieldName);
			Field[] fields = invokeObject.getClass().getFields();
			for (int i = 0; i < fields.length; i++) {
				if (fields[i].getName().equalsIgnoreCase(fieldName)) {
					return fields[i];
				}
			}
			throw new NoSuchFieldException(fieldName);
		}
	}

	// ----------------------------------------------------------------『 その他 』

	/**
	 * オブジェクトから完全修飾していないクラス名を取得します。
	 * @param object
	 * @return
	 */
	public static String getShortClassName(Object object) {
		if (object == null) {
			return "null";
		}
		String name = object.getClass().getName();
		return getShortClassName(name);
	}

	/**
	 * 完全修飾名からクラス名を取得します。
	 * @param className
	 * @return
	 */
	public static String getShortClassName(String className) {
		int index = className.lastIndexOf(".");
		return className.substring(index + 1);
	}

	/**
	 * メソッド名からフィールド名を変えします。 JavaBeansの慣例に適合している 必要があります。
	 * @param methodName
	 * @return
	 */
	public String getFieldName(String methodName) {
		String fieldName = null;
		if (methodName.startsWith("is")) {
			fieldName = methodName.substring(2);
		} else {
			fieldName = methodName.substring(3);
		}
		fieldName = convString(fieldName, 0, "L");
		return fieldName;
	}

	/**
	 * 完全修飾名「className」が存在するクラス名かを検証します。
	 * @param className
	 * @return
	 */
	public boolean isClassExist(String className) {
		try {
			Class.forName(className);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private final static Map beanInfoCache = new HashMap();

	/**
	 * 「object」のオブジェクト情報を保持するPropertyDescriptorを返します。
	 * @param object
	 * @return
	 * @throws IntrospectionException
	 */
	@SuppressWarnings("unchecked")
	public static PropertyDescriptor[] getPropertyDescriptors(Object object)
			throws IntrospectionException {

		BeanInfo beanInfo = (BeanInfo) beanInfoCache.get(object.getClass());
		if (beanInfo == null) {
			beanInfo = Introspector.getBeanInfo(object.getClass());
			beanInfoCache.put(object.getClass(), beanInfo);
		}
		// BeanInfo beanInfo = Introspector.getBeanInfo(object.getClass());
		return beanInfo.getPropertyDescriptors();
	}

	// --------------------------------------------------------------------------
	// ---------------------------------------------『 以下プライベートメソッド 』
	// --------------------------------------------------------------------------
	/**
	 * PropertyDescriptorを元に、引数のfieldNameのアクセサメッソドを サーチします。
	 * @param invokeObject 実行対象のオブジェクト
	 * @param fieldName フィールド名
	 * @param type ゲッターメソッド ⇒ GET ゲッターメソッド ⇒ SET
	 * @return 指定された引数の条件に一致するMethod オブジェクト
	 * @throws NoSuchMethodException 一致するメソッドが見つからない場合、 あるいは名前が "" または "" の場合
	 * @throws IntrospectionException
	 */
	private static Method searchMethod(Object invokeObject, String fieldName,
			String type) throws NoSuchMethodException, IntrospectionException {
		Method method = null;
		fieldName = checkFieldName(fieldName);
		PropertyDescriptor[] props = getPropertyDescriptors(invokeObject);
		for (int i = 0; i < props.length; i++) {
			String name = props[i].getName();
			if (!name.equalsIgnoreCase(fieldName)) {
				continue;
			}
			if (type.equals(GET)) {
				method = props[i].getReadMethod();
			} else {
				method = props[i].getWriteMethod();
			}
			if (method == null) {
				continue;
			}
			return method;
		}

		// メソッドが存在しない場合。
		throw new NoSuchMethodException("クラスにメソッドがありません。"
				+ "（大文字小文字の区別なしです。）: " + type.toLowerCase()
				+ convString(fieldName, 0, "U") + "()");
	}

	/**
	 * 引数のfieldNameがカラム名の場合をチェックし、カラム名の場合は コンバートし返します。
	 *
	 * MAIL_ADDRESS ⇒ MAILADDRESS ↓ mailaddress = mailAddress
	 * @param fieldName フィールド名または、カラム名
	 * @return フィールド名
	 */
	private static String checkFieldName(String fieldName) {
		int index = fieldName.indexOf("_");
		while (true) {
			if (index == -1) {
				return fieldName;
			}
			StringBuffer convcloumn = new StringBuffer(fieldName);
			convcloumn.deleteCharAt(index);
			fieldName = convcloumn.toString();
			index = fieldName.indexOf("_");
		}
	}

	/**
	 * 変換対象のオブジェクト object を convClassName の型へ変換します。
	 *
	 * @param object 変換対象のオブジェクト
	 * @param convClassName 変換する型のクラス文字列
	 * @return 変換されたオブジェクト
	 */
	private static Object convObject(Object object, String convClassName) {
		if (object == null) {
			// プリミティブな型への変換はnullで返すとエラーになる。
			// 0のラッパーにする。
			if (convClassName.equals("int")) {
				return new Integer(0);
			} else if (convClassName.equals("long")) {
				return new Long(0);
			} else {
				return null;
			}
		}
		if (object.getClass().getName().equals(convClassName)) {
			return object;
		}

		// ----------------------------------------『 object instanceof String 』
		if (object instanceof String) {
			if (convClassName.equals("java.lang.String")) {
				return object;
			} else if (convClassName.equals("java.lang.Long")
					|| convClassName.equals("long")) {
				String str = (String) object;
				if (isExist(str)) {
					// 一度BigDecimalに変換しないと具合が悪い
					// 1000.00000
					BigDecimal big = new BigDecimal(str);
					return new Long(big.longValue());
				} else {
					// str が殻リテラルの場合は初期値の"0"を
					return new Long(0);
				}
			} else if (convClassName.equals("java.sql.Date")) {
				return toSqlDate((String) object);
			} else if (convClassName.equals("java.util.Date")) {
				return toUtilDate((String) object);
			} else if (convClassName.equals("java.sql.Timestamp")
					|| convClassName.equals("java.util.Timestamp")) {
				Date date = toSqlDate((String) object);
				return new Timestamp(date.getTime());
			} else if (convClassName.equals("java.lang.Integer")
					|| convClassName.equals("int")) {
				// str が殻リテラルの場合は初期値の"0"を
				String str = (String) object;
				if (isExist(str)) {
					BigDecimal big = new BigDecimal(str);
					return new Integer(big.intValue());
				} else {
					return new Integer(0);
				}
			} else if (convClassName.equals("boolean")
					|| convClassName.equals("java.lang.Boolean")) {
				return Boolean.valueOf(object.toString());
			} else if (convClassName.equals("java.math.BigDecimal")) {
				String temp = ((String) object).trim();
				// temp.length() == 0の場合0ではなくnullにするのが無難。
				if (temp.length() == 0) {
					return null;
				} else {
					return new BigDecimal(temp);
				}
			}
			throwNoSupprt(object, convClassName);
		}

		// ---------------------------------『 object instanceof java.sql.Date 』
		else if (object instanceof java.sql.Date) {

			if (convClassName.equals("java.lang.String")) {
				return toStringDate((java.sql.Date) object, "yyyy/MM/dd");
			} else if (convClassName.equals("java.sql.Date")) {
				return object;
			} else if (convClassName.equals("java.sql.Timestamp")) {
				return new Timestamp(((Date) object).getTime());
			}
			throwNoSupprt(object, convClassName);
		}

		// -------------------------------------『 object instanceof Timestamp 』
		else if (object instanceof Timestamp) {
			long time = ((Timestamp) object).getTime();
			if (convClassName.equals("java.lang.String")) {
				return toStringDate(time, "yyyy/MM/dd HH:mm:ss");
			} else if (convClassName.equals("java.sql.Date")) {
				return new java.sql.Date(time);
			} else if (convClassName.equals("java.sql.Timestamp")) {
				return object;
			}
			throwNoSupprt(object, convClassName);
		}

		// ----------------------------------------『 object instanceof Integer 』
		else if (object instanceof Integer) {
			if (convClassName.equals("java.lang.Integer")
					|| convClassName.equals("int")) {
				return object;
			} else if (convClassName.equals("java.lang.String")) {
				return object.toString();
			} else if (convClassName.equals("java.lang.Long")
					|| convClassName.equals("long")) {
				return new Long(((Integer) object).longValue());
			} else if (convClassName.equals("java.math.BigDecimal")) {
				return new BigDecimal(((Integer) object).intValue());
			}
			throwNoSupprt(object, convClassName);
		}

		// ------------------------------------------『 object instanceof Long 』
		else if (object instanceof Long) {
			if (convClassName.equals("java.lang.Long")
					|| convClassName.equals("long")) {
				return object;
			} else if (convClassName.equals("java.lang.String")) {
				return object.toString();
			} else if (convClassName.equals("java.lang.Integer")
					|| convClassName.equals("int")) {
				return new Integer(((Long) object).intValue());
			} else if (convClassName.equals("java.math.BigDecimal")) {
				return new BigDecimal(((Long) object).longValue());
			}
			throwNoSupprt(object, convClassName);
		}

		// ----------------------------------------『 object instanceof Double 』
		else if (object instanceof Double) {
			if (convClassName.equals("java.lang.String")) {
				// COLUMN NUMBER(8,0)
				// windows oracle > BigDecimal
				// UNIX oracle > Double
				BigDecimal big = new BigDecimal(((Double) object).doubleValue());
				int scale = big.scale();
				if (scale == 0) {
					return big.toString();
				} else {
					// 丸めが必要な場合はサポートしない。
					throwNoSupprt(object, convClassName);
				}
			}
			if (convClassName.equals("java.lang.Integer")
					|| convClassName.equals("int")) {
				return new Integer(((Double) object).intValue());
			} else if (convClassName.equals("java.lang.Long")
					|| convClassName.equals("long")) {
				return new Long(((Double) object).longValue());
			} else if (convClassName.equals("java.math.BigDecimal")) {
				return new BigDecimal(((Double) object).doubleValue());
			}
			throwNoSupprt(object, convClassName);
		}

		// ------------------------------------『 object instanceof BigDecimal 』
		else if (object instanceof BigDecimal) {
			if (convClassName.equals("java.lang.String")) {
				return object.toString();
			} else if (convClassName.equals("java.lang.Long")
					|| convClassName.equals("long")) {
				return new Long(((BigDecimal) object).longValue());
			} else if (convClassName.equals("java.lang.Integer")
					|| convClassName.equals("int")) {
				return new Integer(((BigDecimal) object).intValue());
			}
			throwNoSupprt(object, convClassName);
		}

		// ----------------------------------------『 object instanceof byte[] 』
		else if (object instanceof byte[]) {
			if (convClassName.equals("java.sql.Blob")) {
				return object;
			}
			throwNoSupprt(object, convClassName);
		}

		// ------------------------------------------------『 object が Boolean 』
		else if (object instanceof Boolean) {
			if (convClassName.equals("boolean")) {
				return object;
			}
			throwNoSupprt(object, convClassName);
		}

		// ----------------------------------------------『 object が boolean[] 』
		else if (object instanceof boolean[]) {
			if (convClassName.equals("java.lang.String")) {
				boolean[] bs = (boolean[]) object;
				StringBuffer buff = new StringBuffer("[");
				for (int i = 0; i < bs.length; i++) {
					buff.append(bs[i] + ",");
				}
				buff.deleteCharAt(buff.length() - 1);
				buff.append("]");
				return buff.toString();
			}
			throwNoSupprt(object, convClassName);
		}
		throwNoSupprt(object, convClassName);
		return null;

	}

	/**
	 * 変換がサポートされていない場合にスローします。
	 *
	 * @param object 変換対象のオブジェクト
	 * @param convClassName 変換する型
	 */
	private static void throwNoSupprt(Object object, String convClassName) {
		String className = (object != null) ? object.getClass().getName()
				: "null";
		String errorMess = "\nこのObjectの型変換処理はまだサポートされていません。\n"
				+ " [ Object ] = " + object + ",[ Objectの型 ] = " + className
				+ ",[ convertClass ] = " + convClassName + "";
		throw new UnsupportedOperationException(errorMess);
	}

	/**
	 * 文字列[str]に対して[index]の位置にある文字を大文字または小文字に変換します。
	 *
	 * @param str 評価対象の文字列
	 * @param index 指定する位置
	 * @param toCase 大文字に変換 ⇒ U | u 小文字に変換 ⇒ L | l
	 * @return 変換後の文字列
	 */
	private static String convString(String str, int index, String toCase) {
		if (str == null || str.trim().length() == 0) {
			return str;
		} else {
			String temp = str.substring(index, index + 1);
			if (toCase.equalsIgnoreCase("u")) {
				temp = temp.toUpperCase();
			} else {
				temp = temp.toLowerCase();
			}
			StringBuffer tempBuffer = new StringBuffer(str);
			tempBuffer.replace(index, index + 1, temp);
			return tempBuffer.toString();
		}
	}

	/**
	 * [value]が有効な値かどうか検証します。
	 *
	 * @param value 評価対象文字列
	 * @return [true]:nullでなく""でない場合
	 */
	private static boolean isExist(String value) {
		if (value != null && value.length() != 0) {
			return true;
		}
		return false;
	}

	/**
	 * java.util.Dateクラスまたは、そのサブクラスを指定のフォーマットで 文字列に変換します。
	 * @param date 変換対象のjava.util.Dateクラス
	 * @param pattern 指定のフォーマット
	 * @return フォーマットされた日付文字列
	 */
	private static String toStringDate(Date date, String pattern) {
		SimpleDateFormat sdFormat = new SimpleDateFormat(pattern);
		return sdFormat.format(date);
	}

	private static java.sql.Date toSqlDate(String strDate) {
		Calendar cal = toCalendar(strDate);
		return toSqlDate(cal);
	}

	private static java.sql.Date toSqlDate(Calendar cal) {
		long l = cal.getTime().getTime();
		return new java.sql.Date(l);
	}

	private static java.util.Date toUtilDate(String strDate) {
		Calendar cal = toCalendar(strDate);
		return toUtilDate(cal);
	}

	private static java.util.Date toUtilDate(Calendar cal) {
		long l = cal.getTime().getTime();
		return new java.util.Date(l);
	}

	/**
	 * 時間のロング値を指定のフォーマットで文字列に変換します。
	 * @param time 現在時刻のミリ秒を表すロング値
	 * @param pattern 指定のフォーマット
	 * @return フォーマットされた日付文字列
	 */
	private static String toStringDate(long time, String pattern) {
		return toStringDate(new Date(time), pattern);
	}

	/**
	 * String ⇒ java.sql.Date
	 *
	 * 以下の日付文字列をjava.sql.Dateに変換 yyyy/MM/dd HH:mm:ss.SSS yyyy-MM-dd HH:mm:ss.SSS
	 *
	 * "20030407" "2003/04/07" "2003-04-07" "2003/04/07 15:20:16" "2003-04-07
	 * 15:20:16"
	 * @param strDate
	 * @return
	 */
	private static Calendar toCalendar(String strDate) {
		strDate = format(strDate);
		Calendar cal = Calendar.getInstance();

		int yyyy = Integer.parseInt(strDate.substring(0, 4));
		int MM = Integer.parseInt(strDate.substring(5, 7));
		int dd = Integer.parseInt(strDate.substring(8, 10));
		int HH = cal.get(Calendar.HOUR_OF_DAY);
		int mm = cal.get(Calendar.MINUTE);
		int ss = cal.get(Calendar.SECOND);
		int SSS = cal.get(Calendar.MILLISECOND);

		cal.clear();
		cal.set(yyyy, MM - 1, dd);

		int len = strDate.length();
		switch (len) {
		case 10:
			break;
		case 16: // yyyy/MM/dd HH:mm
			HH = Integer.parseInt(strDate.substring(11, 13));
			mm = Integer.parseInt(strDate.substring(14, 16));
			cal.set(Calendar.HOUR_OF_DAY, HH);
			cal.set(Calendar.MINUTE, mm);
			break;
		case 19: // yyyy/MM/dd HH:mm:ss
			HH = Integer.parseInt(strDate.substring(11, 13));
			mm = Integer.parseInt(strDate.substring(14, 16));
			ss = Integer.parseInt(strDate.substring(17, 19));
			cal.set(Calendar.HOUR_OF_DAY, HH);
			cal.set(Calendar.MINUTE, mm);
			cal.set(Calendar.SECOND, ss);
			break;
		case 23: // yyyy/MM/dd HH:mm:ss.SSS
			HH = Integer.parseInt(strDate.substring(11, 13));
			mm = Integer.parseInt(strDate.substring(14, 16));
			ss = Integer.parseInt(strDate.substring(17, 19));
			SSS = Integer.parseInt(strDate.substring(20, 23));
			cal.set(Calendar.HOUR_OF_DAY, HH);
			cal.set(Calendar.MINUTE, mm);
			cal.set(Calendar.SECOND, ss);
			cal.set(Calendar.MILLISECOND, SSS);
			break;
		default:
			throw new IllegalStateException("このString文字列は日付文字列に変換できません : "
					+ strDate);
		}
		return cal;
	}

	/**
	 * あらゆる日付文字列を"yyyy/MM/dd" or "yyyy/MM/dd HH:mm:ss"の フォーマットに変換することを試みます。
	 * 例：03/1/3 ⇒ 2003/01/03
	 * @param strDate
	 * @return
	 */
	private static String format(String strDate) {
		strDate = strDate.trim();
		String yyyy = null;
		String MM = null;
		String dd = null;
		String HH = null;
		String mm = null;
		String ss = null;
		String SSS = null;

		// "-" or "/" が無い場合
		if (strDate.indexOf("/") == -1 && strDate.indexOf("-") == -1) {
			if (strDate.length() == 8) {
				yyyy = strDate.substring(0, 4);
				MM = strDate.substring(4, 6);
				dd = strDate.substring(6, 8);
				return yyyy + "/" + MM + "/" + dd;
			} else {
				yyyy = strDate.substring(0, 4);
				MM = strDate.substring(4, 6);
				dd = strDate.substring(6, 8);
				HH = strDate.substring(9, 11);
				mm = strDate.substring(12, 14);
				ss = strDate.substring(15, 17);
				return yyyy + "/" + MM + "/" + dd + " " + HH + ":" + mm + ":"
						+ ss;
			}
		}
		StringTokenizer token = new StringTokenizer(strDate, "_/-:. ");
		StringBuffer result = new StringBuffer();
		for (int i = 0; token.hasMoreTokens(); i++) {
			String temp = token.nextToken();
			switch (i) {
			case 0:// 年の部分
				yyyy = fillString(strDate, temp, "f", "20", 4);
				result.append(yyyy);
				break;
			case 1:// 月の部分
				MM = fillString(strDate, temp, "f", "0", 2);
				result.append("/" + MM);
				break;
			case 2:// 日の部分
				dd = fillString(strDate, temp, "f", "0", 2);
				result.append("/" + dd);
				break;
			case 3:// 時間の部分
				HH = fillString(strDate, temp, "f", "0", 2);
				result.append(" " + HH);
				break;
			case 4:// 分の部分
				mm = fillString(strDate, temp, "f", "0", 2);
				result.append(":" + mm);
				break;
			case 5:// 秒の部分
				ss = fillString(strDate, temp, "f", "0", 2);
				result.append(":" + ss);
				break;
			case 6:// ミリ秒の部分
				SSS = fillString(strDate, temp, "b", "0", 3);
				result.append("." + SSS);
				break;
			}
		}
		return result.toString();
	}

	private static String fillString(String strDate, String str,
			String position, String addStr, int len) {

		if (str.length() > len) {
			String mes = strDate + "このString文字列は日付文字列に変換できません";
			throw new IllegalStateException(mes);
		}
		return fillString(str, position, addStr, len);
	}

	/**
	 * 文字列[str]に対して補充する文字列[addStr]を[position]の位置に[len]に 満たすまで挿入します。
	 *
	 * 例： String ss = StringUtil.fillString("aaa","b","0",7); ss ⇒ "aaa0000"
	 *
	 * ※fillString()はlenに満たすまで挿入しますが、addString()はlen分挿入します。
	 *
	 * @param str 対象文字列
	 * @param position 前に挿入 ⇒ F/f 後に挿入 ⇒ B/b
	 * @param addStr 挿入する文字列
	 * @param len 補充するまでの桁数
	 * @return 変換後の文字列。 [str]がnullや空リテラルも[addStr]を[len]に 満たすまで 挿入した結果を返します。
	 */
	private static String fillString(String str, String position,
			String addStr, int len) {
		StringBuffer tempBuffer = null;
		if (!isExist(str)) {
			tempBuffer = new StringBuffer();
			for (int i = 0; i < len; i++) {
				tempBuffer.append(addStr);
			}
			return tempBuffer.toString();
		} else if (str.length() != len) {
			tempBuffer = new StringBuffer(str);
			while (len > tempBuffer.length()) {
				if (position.equalsIgnoreCase("f")) {
					tempBuffer.insert(0, addStr);
				} else {
					tempBuffer.append(addStr);
				}
			}
			return tempBuffer.toString();
		}
		return str;
	}

	// --------------------------------------------------------------------------
	// ---------------------------------------------『 ファクト生成メソッド 』
	// --------------------------------------------------------------------------

	/**
	 *
	 * ファイルからファクトの値を取得する
	 *
	 * @param inputFileList
	 *            ファクト値取得ファイル
	 * @return ファクト値MAP
	 * @throws EncryptedDocumentException
	 * @throws InvalidFormatException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public Map<String, List<List<String>>> getValueInstance(File res)
			throws EncryptedDocumentException, InvalidFormatException, FileNotFoundException, IOException {

		// 移行元クラス名の読み込み
		List<String> classNames = getClassNameList(1, settings.getInPath());

		// listFilesメソッドを使用して入力値ファイルの一覧を取得する
		File[] inputFileList = res.listFiles();
		if (inputFileList == null) {
			String errMsg = res + " 内にファイルが見つかりません。";
			throw new FileNotFoundException(errMsg);
		}

		Map<String, List<List<String>>> param = new LinkedHashMap<>();

		for (File file : inputFileList) {

			// 共通インターフェースを扱える、WorkbookFactoryで読み込む
			Workbook workbook = null;
			try {
				workbook = openWorkbook(file);
			} catch (org.apache.poi.openxml4j.exceptions.InvalidFormatException e) {
				System.err.println(e.getMessage());
				throw new ApplicationException("予期せぬエラーが発生しました。", e);
			}

			dataFormatter = new DataFormatter();
			formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();

			CellAddress address = null;

			// シート取得
			List<Sheet> sheetList = getSheet(workbook, classNames);

			for (Sheet sheet : sheetList) {

				List<List<String>> inputVal = new ArrayList<>();

				// ファクトクラス名を取得
				address = new CellAddress(Constants.Excel.ROW_OF_ENTITY_NAME, Constants.Excel.COL_OF_ENTITY_NAME);
				String className = getCellValue(sheet, address).trim();

				int maxColNum = getMaxCol(sheet);

				// 行毎の値を取得
				for (int rownum = Constants.Excel.ROW_OF_CELL_START; rownum < sheet.getLastRowNum() + 1; rownum++) {
					address = new CellAddress(rownum, Constants.Excel.COL_OF_CELL_START);
					if (Objects.equals(getCellValue(sheet, address), null)
							|| Objects.equals(getCellValue(sheet, address), Constants.Character.BLANK)) {
						break;
					}
					List<String> rowVal = new ArrayList<>();
					for (int colnum = Constants.Excel.COL_OF_CELL_START; colnum < maxColNum; colnum++) {
						address = new CellAddress(rownum, colnum);
						rowVal.add(getCellValue(sheet, address));
					}
					// 行データ取得
					inputVal.add(rowVal);
				}
				param.put(className, inputVal);
			}
			workbook.close();
		}

		return param;
	}

	/**
	 *
	 * 値取得の最大列数を取得
	 *
	 * @param sheet
	 *            シート
	 * @return
	 */
	private static int getMaxCol(Sheet sheet) {

		int colnum = 0;
		Row rowTitle = sheet.getRow(Constants.Excel.ROW_OF_CELL_START);

		for (int num = Constants.Excel.COL_OF_CELL_START; num < rowTitle.getLastCellNum(); num++) {
			CellAddress address = new CellAddress(Constants.Excel.ROW_OF_CELL_START, num);
			if (Objects.equals(getCellValue(sheet, address), null)
					|| Objects.equals(getCellValue(sheet, address), Constants.Character.BLANK)) {
				// 項目値取得の最大列番号
				colnum = num;
				break;
			}
		}
		return colnum;
	}

	/**
	 * ファクトの値を読み取る対象シートを取得する
	 *
	 * @param Workbook
	 *            ワークブック
	 *
	 * @return sheet ファクトの値を読み取る対象シート
	 *
	 */
	private static List<Sheet> getSheet(Workbook workbook, List<String> classNames) {
		List<Sheet> sheet = new ArrayList<Sheet>();
		for (Sheet entry : workbook) {
			if (classNames.contains(entry.getSheetName())) {
				sheet.add(entry);
			}
		}
		return sheet;
	}

	/**
	 * Excelファイルをオープンする.
	 *
	 * @param file
	 *            Excelファイル
	 * @return ワークブック
	 * @throws EncryptedDocumentException
	 * @throws InvalidFormatException
	 * @throws IOException
	 * @throws org.apache.poi.openxml4j.exceptions.InvalidFormatException
	 */
	protected static Workbook openWorkbook(File file)
			throws EncryptedDocumentException, InvalidFormatException, IOException,
			org.apache.poi.openxml4j.exceptions.InvalidFormatException {
		return WorkbookFactory.create(file, null, true);
	}

	/**
	 * セルの取得時に再計算をせずに記録されている結果を使うかを取得する.
	 * @return キャッシュされている結果を使う場合は true、それ以外は false.
	 */
	public boolean isUseCachedFormulaResult() {
		return useCachedFormulaResult;
	}

	/**
	 * セルの取得時に再計算をせずに記録されている結果を使うかを指定する.
	 * @param useCachedFormulaResult キャッシュされている結果を使う場合は true、それ以外は false.
	 */
	//	public void setUseCachedFormulaResult(boolean useCachedFormulaResult) {
	//		this.useCachedFormulaResult = useCachedFormulaResult;
	//	}

	/**
	 * セルの値を取得する.
	 *
	 * @param row 行
	 * @param columnIndex 列のインデックス
	 * @return セルの値. セルが存在しない場合は null.
	 */
	public static String getCellValue(Row row, int columnIndex) {
		if (row == null) {
			return null;
		}

		Cell cell = row.getCell(columnIndex);
		if (cell == null) {
			return null;
		}

		if (useCachedFormulaResult && cell.getCellTypeEnum() == CellType.FORMULA) {
			CellType cachedType = cell.getCachedFormulaResultTypeEnum();
			switch (cachedType) {
			case BOOLEAN:
				return Boolean.toString(cell.getBooleanCellValue());
			case NUMERIC:
				return Double.toString(cell.getNumericCellValue());
			case STRING:
				return cell.getStringCellValue();
			case ERROR:
				return FormulaError.forInt(cell.getErrorCellValue()).getString();
			default:
				// ここは無い
			}
		}
		return dataFormatter.formatCellValue(cell, formulaEvaluator);
	}

	/**
	 * セルの値を int で取得する.
	 *
	 * @param row 行
	 * @param columnIndex 列のインデックス
	 * @return セルの値. セルが存在しない場合は null.
	 */
	public Integer getIntegerCellValue(Row row, int columnIndex) {
		String value = getCellValue(row, columnIndex);
		if (StringUtils.isEmpty(value) || value.equals("#N/A")) {
			return null;
		}
		return new BigDecimal(value).intValue();
	}

	/**
	 * セルの値を取得する.
	 *
	 * @param sheet シート
	 * @param address セルのアドレス
	 * @return セルの値. セルが存在しない場合は null.
	 */
	public static String getCellValue(Sheet sheet, CellAddress address) {
		return getCellValue(sheet.getRow(address.getRow()), address.getColumn());
	}

}
