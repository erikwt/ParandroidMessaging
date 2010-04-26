import java.math.BigInteger;
import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;




public class testEncryptionOverhead {

	protected static final String LIPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc in massa sit amet odio iaculis ultrices porta at velit. Nulla imperdiet, elit quis scelerisque accumsan, felis lacus interdum nisl, nec fringilla sapien tortor vel dolor. Fusce dictum, erat sed faucibus vulputate, nibh dolor blandit nulla, eu sagittis libero eros ut ante. Duis dictum pharetra dui vel elementum. Mauris ac diam ac eros adipiscing dictum non sed tellus. Nulla facilisi. Quisque dui neque, fringilla at sodales ut, luctus a tortor. Ut sollicitudin dapibus malesuada. Nullam mattis, nisi vitae auctor hendrerit, magna erat semper dolor, sit amet semper magna ipsum id nibh. Morbi luctus eros ut tortor mollis aliquam. Phasellus sed erat nec purus aliquam vestibulum. Quisque id lorem ac sem placerat viverra. Donec sem sapien, gravida at vehicula vel, rhoncus et massa. Praesent eget elementum libero. Phasellus in erat vel est mattis dapibus.Duis ut consequat justo. Donec purus risus, luctus sed rutrum sed, tempus sed orci. Nunc mattis turpis at enim porttitor ut aliquet ante imperdiet. Duis non sem lorem, non fermentum mi. Sed ut neque in orci bibendum tempus. Ut sagittis, magna eget dictum dignissim, libero lectus malesuada velit, sed interdum mi mauris non quam. Etiam in risus cursus libero semper rutrum. Pellentesque purus urna, pharetra vitae eleifend a, consectetur non lectus. In id dui enim, id imperdiet orci. Sed ut turpis vel nisl gravida eleifend ac non sapien.Pellentesque eu libero vel odio rutrum sodales sed mollis nulla. Vivamus dapibus tincidunt erat a placerat. Pellentesque non risus eu nibh fermentum molestie sed in sapien. Proin tincidunt nunc id mi varius sed cursus lorem adipiscing. Nulla tincidunt, eros quis pretium molestie, lacus neque posuere turpis, porttitor eleifend sem odio nec tortor. Aenean volutpat orci et lacus condimentum sed fringilla elit mattis. Sed id malesuada purus. Proin dolor felis, congue vitae interdum aliquet, elementum blandit mi. Nulla non mauris at orci bibendum tincidunt a sed elit. Morbi dignissim, leo sodales blandit sagittis, libero velit pharetra risus, et dapibus ipsum urna vel libero. Sed pharetra lectus nec felis consequat vehicula egestas quam posuere. Etiam nec libero id magna sagittis viverra. Fusce quis nulla eget urna luctus pharetra sit amet eget massa. Vivamus ac velit et lectus cursus convallis venenatis eu lectus.Curabitur gravida, massa a vehicula dignissim, augue neque vestibulum dolor, vel congue metus enim nec arcu. Donec sit amet luctus metus. Nulla vehicula faucibus justo, et cursus dui rhoncus aliquet. Sed ut odio ut turpis fringilla egestas sit amet at risus. Praesent quis ipsum sit amet est elementum egestas a ac dui. Morbi et diam dictum eros consectetur congue. Ut eget felis non felis iaculis fringilla vel et turpis. Vestibulum ut ligula enim. Nulla ut ornare odio. Praesent orci purus, bibendum vel congue nec, posuere et nisl. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Curabitur nec nulla vel nibh cursus cursus sed vel tellus. Sed laoreet ligula non nisi placerat vitae convallis elit placerat. Morbi pharetra, neque at dignissim lacinia, dui nisl egestas ligula, ut eleifend erat ante ac neque. Quisque dictum nibh sed ipsum lobortis malesuada. Proin dignissim placerat diam, tincidunt ultrices lorem auctor in. Donec placerat ullamcorper fermentum. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.Vivamus a turpis ut orci accumsan tempor. Duis vel tortor lorem. In quis augue eget orci porta dignissim. Donec sem nulla, convallis ut accumsan nec, scelerisque in nulla. Mauris a nisi arcu, a placerat ipsum. Nullam aliquet laoreet leo ut venenatis. Mauris semper cursus metus. Vivamus adipiscing diam vel nibh rutrum fringilla. Mauris mattis porta lacus. Maecenas mattis lacinia tincidunt. Quisque sit amet metus consequat felis vehicula auctor.Mauris vestibulum, tortor et fermentum mattis, purus ante venenatis massa, ut faucibus erat sem vel lacus. Quisque magna diam, sagittis vitae posuere vitae, convallis rhoncus neque. Duis adipiscing posuere elit id vehicula. Sed risus metus, interdum sed scelerisque ut, dignissim eu velit. Aliquam erat volutpat. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed neque ligula, elementum a egestas ut, lacinia vitae ipsum. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Lorem ipsum dolor sit amet, consectetur adipiscing elit. In eu mauris nec dui posuere consectetur. Nullam lacus est, venenatis non iaculis nec, rhoncus vel sapien. Cras eget felis neque, sed dignissim dolor. Praesent sollicitudin dapibus nunc, ut porta tellus pellentesque eget. Pellentesque ac nisi non tortor dignissim ornare vel sed erat. Maecenas at ligula ut lacus dignissim molestie. Cras a tellus vitae justo fermentum mattis id vitae nulla. Ut eu lacus leo, eu suscipit justo. Nulla ut lacus risus, quis varius tortor. Ut vitae dui nec felis egestas aliquet.Proin dolor ligula, faucibus in sollicitudin ut, luctus et lectus. Vivamus cursus, metus ac aliquam ornare, dolor velit tempor lorem, et tempus orci massa a elit. Phasellus nec erat id enim laoreet venenatis auctor vel tellus. Nullam lorem nisi, consequat at dapibus quis, luctus id elit. Aliquam rutrum, nunc sit amet viverra adipiscing, enim arcu auctor metus, consectetur pretium justo mi sed velit. Morbi ornare dolor non ipsum consequat vel fermentum diam interdum. Donec imperdiet lorem vitae eros ullamcorper fringilla. Curabitur mollis interdum urna ac facilisis. Vivamus non ligula nec leo porttitor varius. Morbi fermentum imperdiet mauris. Quisque et turpis neque. Praesent molestie eleifend purus, ut laoreet dolor gravida et. Cras semper, augue a viverra congue, nulla leo convallis urna, rutrum euismod felis libero quis ipsum. Donec id porttitor felis. Suspendisse sit amet est lorem, nec egestas sem. Suspendisse at velit lorem. Ut lobortis pellentesque tortor, ac bibendum nunc dapibus ac. Donec ac quam at est pulvinar commodo id at quam.Curabitur nec lacus sed est rhoncus feugiat. Etiam et quam non eros fringilla suscipit in eget sapien. Donec ut felis risus, id porta magna. Phasellus vel ante id ante facilisis luctus. Donec quis metus ut lectus tempus ultricies. Suspendisse nibh tellus, volutpat et rutrum vel, iaculis nec neque. Nulla at elit eu elit venenatis varius et in lacus. Phasellus bibendum tellus quis dolor luctus egestas. Nulla ac ullamcorper lacus. Proin nec magna a erat tincidunt egestas. Mauris vel leo mi, vel lacinia libero. Sed dictum, nisl sagittis pharetra scelerisque, eros massa interdum diam, eget tincidunt ante leo vitae lacus. Proin tortor justo, blandit ut faucibus ut, ultrices vel ante.Etiam vel diam enim. Vestibulum lacinia lectus risus. Aenean adipiscing eros id lacus facilisis vitae venenatis nulla sagittis. Integer mollis sapien sit amet massa faucibus pellentesque. Donec viverra venenatis tincidunt. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Suspendisse suscipit, est vel dapibus consequat, ante leo molestie mi, eget pellentesque nisi neque a odio. Etiam a mi elit, eu ultricies nisl. In vel aliquam eros. Fusce tincidunt nisi ac arcu sagittis in condimentum est ultricies.Etiam ut justo non turpis aliquet tempor. Ut malesuada, nibh non facilisis accumsan, diam turpis blandit nisl, non pellentesque augue nisl ut nibh. Ut vitae vulputate neque. Nullam id est eu neque congue tempus. Quisque viverra sapien at nulla gravida ac gravida augue ornare. Aenean commodo, nulla ut placerat rutrum, purus magna viverra augue, non hendrerit nulla urna eu enim. Nulla facilisi. Pellentesque consectetur porttitor ante, eget interdum massa rutrum venenatis. Donec nec tortor odio, quis fringilla risus. Aenean accumsan, nunc a ultricies dignissim, tortor nisi gravida mauris, luctus condimentum velit enim sed dolor. Duis consequat lacus id quam pretium fringilla. Vivamus bibendum ipsum ac nisi ornare a rhoncus ligula tincidunt. Integer a sem lectus, nec sagittis nulla.Maecenas non velit eget diam tincidunt rutrum. Fusce rutrum, diam sit amet aliquet porttitor, ligula ipsum interdum turpis, eu cursus sem lectus sed erat. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Lorem ipsum dolor sit amet, consectetur adipiscing elit. Mauris in nisl diam, nec tempus tellus. Donec convallis urna quis dolor mollis at tincidunt nisl faucibus. Sed quis consequat risus. Maecenas non turpis id mauris consectetur aliquet. Donec et libero sapien. Nulla in quam dui, vitae tempor erat. Donec ornare tellus id odio auctor volutpat. Integer laoreet vehicula dui, at ultrices dolor rhoncus non. Nam adipiscing leo vel ante eleifend feugiat. Sed sed massa eu diam laoreet ornare. Curabitur pharetra neque vitae augue rhoncus ac porta mauris tincidunt. Nunc id lorem sit amet quam luctus vestibulum eu sed velit. Duis pretium viverra purus, porttitor elementum nibh fringilla sit amet.Sed erat neque, blandit ac commodo et, sagittis sit amet augue. Morbi nisi ligula, sodales nec interdum quis, rhoncus et nisi. Mauris sed leo turpis. Nunc dignissim orci quis risus volutpat bibendum. Pellentesque vel ullamcorper nisi. Donec libero eros, accumsan non fermentum ut, scelerisque sed enim. Praesent vestibulum risus ac quam sagittis nec convallis ante convallis. Etiam placerat varius odio eget faucibus. Donec interdum metus vitae nibh imperdiet consectetur sollicitudin sapien convallis. Proin tincidunt consequat diam, vel convallis est condimentum in. Nam semper odio sit amet mauris ornare ut tempus quam facilisis. Etiam quis fringilla massa.Mauris at mi eget mauris tempus porta. Sed ut libero ut erat sagittis rhoncus ac vel nulla. Mauris porta augue velit. Curabitur pretium eleifend magna, hendrerit aliquet massa luctus at. Nullam tempus, tortor non imperdiet dignissim, metus magna laoreet tortor, vitae consectetur risus turpis in velit. Curabitur diam nibh, mollis vel congue pharetra, malesuada nec enim. Mauris vulputate nunc vel elit sodales eget bibendum arcu faucibus. Praesent facilisis ligula dolor, ac molestie risus. Etiam est dolor, sagittis nec molestie at, volutpat vitae leo. Fusce non mauris vitae ante venenatis bibendum. Quisque egestas nisl vel erat auctor ut tempor turpis condimentum. Aliquam vestibulum, massa eget dapibus lobortis, lacus orci posuere nibh, ullamcorper tristique est lorem eget dolor. Pellentesque iaculis dui a metus tincidunt ac ornare nulla vehicula. Quisque eu dolor nec eros dignissim fringilla a non risus. Morbi suscipit leo eu odio porta ullamcorper nec quis dolor. Quisque vehicula, velit a laoreet luctus, lectus libero malesuada risus, venenatis bibendum elit lectus vel ligula. Suspendisse ultrices varius dignissim.Nulla scelerisque ornare dui, nec mattis neque molestie in. Integer eu neque non lacus tincidunt adipiscing et lobortis dolor. Nullam tortor eros, dictum quis ultrices ut, commodo ut quam. Suspendisse tincidunt scelerisque turpis sit amet egestas. Sed massa dolor, ultricies ornare mollis eget, euismod eu urna. Phasellus porta ornare mauris volutpat lacinia. Aenean eu vulputate nunc. Nunc ut aliquam est. Maecenas nisl leo, sodales sed vulputate pellentesque, consequat id nunc. Suspendisse id massa in nulla mattis eleifend. Fusce ut velit metus. Integer sit amet tortor dolor, vel sollicitudin felis. Curabitur venenatis bibendum turpis, nec sollicitudin orci congue id.Nulla at nisl ut dolor interdum varius. Ut id mi quam. Ut odio enim, varius nec ornare nec, facilisis eu nibh. Suspendisse potenti. Nam elementum dictum tortor et placerat. Phasellus rhoncus libero in augue euismod laoreet. Maecenas euismod, sem sit amet mattis ultrices, elit arcu tempor ligula, cursus tincidunt augue dui a risus. Curabitur fringilla elit sed orci blandit congue. Sed sollicitudin orci non libero aliquet tempus. Etiam vehicula odio vel mauris ullamcorper id gravida quam auctor. Vestibulum dignissim enim magna. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur pharetra sollicitudin lacus sit amet fermentum. Pellentesque massa lorem, gravida nec varius ac, consectetur sed purus. Sed rhoncus, risus non vulputate malesuada, justo eros pharetra nisl, eu blandit sem mauris in erat.Nam vehicula interdum arcu, at cursus elit suscipit sit amet. Nullam bibendum lobortis libero, id suscipit ante auctor pellentesque. Aenean dignissim, lacus sed vehicula bibendum, magna turpis euismod ante, id rhoncus eros ipsum sit amet diam. Aenean sed orci tortor. In eget turpis in est sagittis pellentesque eget non dui. Aliquam vitae velit eget quam blandit feugiat ut at mi. Aliquam erat volutpat. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Suspendisse ullamcorper interdum malesuada. Nunc eu ipsum eu tortor accumsan dignissim. Nunc facilisis tincidunt risus suscipit vulputate. Nunc porta egestas imperdiet. Vivamus dictum placerat consequat. Fusce nec ante et augue dapibus fermentum.In a lacus lacus, in venenatis sem. Suspendisse tincidunt laoreet nisl, id sodales elit semper at. Donec fermentum tempus rhoncus. Duis ac dolor at ante elementum viverra sed aliquet nibh. Nunc elementum ligula ut ante aliquam in facilisis elit gravida. Nam eu hendrerit risus. Pellentesque consectetur cursus ipsum, gravida accumsan purus porttitor vitae. Ut adipiscing dignissim velit, nec tincidunt leo vestibulum id. Curabitur molestie, purus eu fermentum interdum, tortor nibh viverra sem, in gravida risus dui in leo. Donec nunc mi, imperdiet eget posuere sed, facilisis a ante. Mauris sodales facilisis justo elementum lacinia. Nulla porta massa ac libero lobortis ullamcorper. Nulla eget lobortis erat. Vestibulum dapibus felis nec magna tristique ullamcorper. In lacinia condimentum lorem, a malesuada massa euismod ut. Quisque sodales urna non purus hendrerit posuere. Sed tempus placerat ante eget molestie. Curabitur vitae mauris turpis, et vestibulum nibh.Sed et bibendum libero. Curabitur mollis cursus nisi ac ultrices. Nullam sed sodales augue. Nullam tristique dapibus purus in bibendum. Donec ullamcorper augue non dolor fermentum eget fermentum leo lacinia. Nullam id arcu nec eros rutrum ornare. Phasellus eget elit nibh, vitae posuere risus. Nam lectus eros, euismod ac laoreet ac, interdum id sem. Cras eu neque urna, sodales fringilla turpis. Fusce mi libero, rhoncus a cursus eget, hendrerit vel enim. Pellentesque placerat elit vitae diam elementum lobortis. Phasellus sollicitudin libero nec dui mattis at eleifend lorem venenatis. Nullam et purus tempor velit aliquam mattis molestie nec eros.Nam id malesuada lorem. Nam vel felis sollicitudin nisl venenatis dictum. Praesent rutrum risus a dolor faucibus vel congue ligula congue. Duis dolor enim, dignissim vitae mattis at, mattis a erat. Curabitur accumsan magna ut arcu pretium imperdiet. Vestibulum eros odio, scelerisque in facilisis et, ultrices quis sapien. Aliquam sit amet libero vitae dolor sollicitudin rutrum rutrum quis nibh. Mauris condimentum varius dolor sit amet aliquam. Sed ac orci lacus. Praesent sed libero nunc, luctus tempor ipsum. Integer sed auctor purus. Integer mi nisl, cursus in volutpat sit amet, ornare id ligula. Vivamus tempus libero ut orci commodo facilisis. Praesent vehicula posuere ipsum, gravida bibendum purus imperdiet vitae. Nulla ultrices, neque at laoreet ultrices, tellus eros sodales dolor, non fringilla arcu mauris at sem.Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Lorem ipsum dolor sit amet, consectetur adipiscing elit. Ut ut pulvinar odio. In ultricies ipsum a dolor varius faucibus. Vestibulum imperdiet enim quis justo viverra faucibus. Etiam nec dolor ac lectus dictum ultrices congue eget neque. Morbi eu ligula lorem, nec ullamcorper purus. Vestibulum quis dictum mi. Pellentesque ante nisl, volutpat in porttitor in, sodales et ante. Suspendisse mollis metus quis lectus porta facilisis dapibus nisl fermentum. Vestibulum convallis fringilla urna, id vehicula dui viverra vitae. Sed auctor imperdiet vulputate. Nulla non velit arcu, eget euismod lacus. Duis ligula metus, faucibus vel imperdiet a, accumsan faucibus dolor. Aenean hendrerit sodales mi, ac eleifend magna feugiat gravida. ";
	protected static final String KEY_EXCHANGE_PROTOCOL = "DH";
	protected static final String ENCRYPTION_ALGORITHM = "DES";
	protected static final String PRIVATE_KEY_ENCRYPTION_ALGORITHM = "PBEWithMD5AndDES";

	private static final BigInteger G = new BigInteger("2");
	private BigInteger P;
	
	protected PrivateKey privateKeyAlice;
	protected PublicKey publicKeyAlice;
	
	protected PrivateKey privateKeyBob;
	protected PublicKey publicKeyBob;
	
	protected SecretKey secretKey;
	
	public testEncryptionOverhead(int keysize){
		try {
//			System.out.println(genDhParams());
			genDhParams(keysize);
			generateKeyPair();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public byte[] encrypt(String text) throws Exception {
//		PrivateKey privateKey = getPrivateKey();
//		PublicKey publicKey  = getPublicKey();
		SecretKey secretKey = generateSecretKey(privateKeyAlice, publicKeyBob);
		
		Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] cipherText = cipher.doFinal(text.getBytes());
		
		return cipherText;
	}
	
	
	public SecretKey generateSecretKey(PrivateKey privateKey, PublicKey publicKey) throws NoSuchAlgorithmException, GeneralSecurityException {
        if(privateKey == null)
        	throw new GeneralSecurityException("Missing private key");
        
        if(publicKey == null)
        	throw new GeneralSecurityException("Missing public key");
    	
    	KeyAgreement ka = KeyAgreement.getInstance(KEY_EXCHANGE_PROTOCOL);
        ka.init(privateKey);
        ka.doPhase(publicKey, true);
        
        SecretKey secretKey = ka.generateSecret(ENCRYPTION_ALGORITHM);
        return secretKey; 
    }
	
//	public PrivateKey getPrivateKey() throws Exception {
//		byte[] keyBytes = privateKey.getEncoded();
//		
//		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
//		KeyFactory kf = KeyFactory.getInstance(KEY_EXCHANGE_PROTOCOL);
//		PrivateKey privateKey = kf.generatePrivate(spec);
//		
//		return privateKey;
//	}
//	
//	public PublicKey getPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException{
//    	byte[] keyBytes = publicKey.getEncoded();
//    	
//    	X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
//        KeyFactory keyFact = KeyFactory.getInstance(KEY_EXCHANGE_PROTOCOL);
//        PublicKey publicKey = keyFact.generatePublic(x509KeySpec);
//    	
//    	return publicKey;
//    }
	
	public void generateKeyPair() throws Exception {  	
    	// Generating keys for alice
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_EXCHANGE_PROTOCOL);
        DHParameterSpec dhSpec = new DHParameterSpec(P, G);
        keyGen.initialize(dhSpec);
        KeyPair keyPair = keyGen.generateKeyPair();

        publicKeyAlice = keyPair.getPublic();
        privateKeyAlice = keyPair.getPrivate();
        
        // Generating keys for bob5
        KeyPairGenerator keyGenBob = KeyPairGenerator.getInstance(KEY_EXCHANGE_PROTOCOL);
        DHParameterSpec dhSpecBob = new DHParameterSpec(P, G);
        keyGenBob.initialize(dhSpecBob);
        KeyPair keyPairBob = keyGenBob.generateKeyPair();

        publicKeyBob = keyPairBob.getPublic();
        privateKeyBob = keyPairBob.getPrivate();
    }
	
	
    private void genDhParams(int keysize) {
        try {
            // Create the parameter generator for a 1024-bit DH key pair
            AlgorithmParameterGenerator paramGen = AlgorithmParameterGenerator.getInstance(KEY_EXCHANGE_PROTOCOL);
            paramGen.init(keysize);
    
            // Generate the parameters
            AlgorithmParameters params = paramGen.generateParameters();
            DHParameterSpec dhSpec
                = (DHParameterSpec)params.getParameterSpec(DHParameterSpec.class);
            
//            return ""+dhSpec.getP()+","+dhSpec.getG()+","+dhSpec.getL();
            P = dhSpec.getP();
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }
    
    public float getAverage(ArrayList<Float> list){
    	float total = 0;
    	Iterator<Float> iterator = list.iterator();
    	while(iterator.hasNext()) total += iterator.next();
    	
    	return total/list.size()*100;
    }
	
	public static void main(String args[]){
		if(args.length != 3) {
			System.out.println("Usage: java test.testEncryptionOverhead keysize maxMsgSize numberOfTries");
			System.exit(1);
		}
		
		int keysize = Integer.parseInt(args[0]);
		testEncryptionOverhead test = new testEncryptionOverhead(keysize);
		
		ArrayList<Float> overheadList = new ArrayList<Float>();
		
		int maxMsgLength = Integer.parseInt(args[1]);
		int numberOfTries = Integer.parseInt(args[2]);
		Random generator = new Random();
		
		for(int i = 0; i < numberOfTries; i++){
			int msgLength = generator.nextInt(maxMsgLength);	// generate a random message length between 0 and maxMsgLength
			int substringStart = generator.nextInt(LIPSUM.length()-maxMsgLength); 	// generate a random start index
			
			String text = LIPSUM.substring(substringStart, substringStart+msgLength);
			
			try {
				byte[] cipherText = test.encrypt(text);
				float textLength = new Float(text.getBytes().length);
				float cipherLength = new Float(cipherText.length);
				
				float overhead = (cipherLength - textLength) / cipherLength;
				overheadList.add(overhead);
				
				System.out.println(" -- Msg size: " + text.getBytes().length + " -- Cipher size: " + cipherText.length + " -- overhead: " + overhead*100 + "%");
//				System.out.println("'" + text + "' --- '" + Base64Coder.encodeString(new String(cipherText)) + "'\n");
			} catch (Exception e) {
				System.out.println("Failed");
			}
			
		}
		
		System.out.println("====================================================");
		System.out.println("= Results:");
		System.out.println("====================================================");
		System.out.println("= Keysize: " + keysize + " bit");
		System.out.println("= Max message length: " + maxMsgLength);
		System.out.println("= Num messages tested: " + numberOfTries);
		System.out.println("= Average overhead: " + test.getAverage(overheadList) + "%");
		System.out.println("====================================================");
		
	}
	
}

