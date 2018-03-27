package pt.ulisboa.tecnico.hdscoin.server;

import java.security.PublicKey;
import java.rmi.RemoteException;

import pt.ulisboa.tecnico.hdscoin.Crypto.CipheredMessage;
import pt.ulisboa.tecnico.hdscoin.Crypto.CryptoManager;
import pt.ulisboa.tecnico.hdscoin.Crypto.Message;
import pt.ulisboa.tecnico.hdscoin.interfaces.*;


public class ServerInterface implements RemoteServerInterface {

	public static final String SERVER_KEY = "30820122300d06092a864886f70d01010105000382010f003082010a0282010100c105187797a1ce79087657d825796562b2143fb7a4f8fd829996ede398f9f3c2103aaf4cba7d10e0322cbd938b8a07b8ac6978db1c23f7b1b609b3bdb41702633d97b064ba74b5498e3850ff01ef9b3b637d4af30ac579ea9f7123cb6e17c5c83751829617e7bbc7a1dc4400bb8d596524572ace113a49ba961bd749e5cb223dfe1a7c0e11799c0e38f59dff5b0e120c66672a079ae1c7c143f5c197d344f45d665dc744e119b837b4a7a10389dba9d7513dbc2e5115d99a5138947738a2895b3b87cb7b21d4637f61b5f0aeaaec7e8c15314e0d6c5d998ecd99bcb0562c1c94c0e956ca7466f9beaf0799bd108a3b468579ca40937747bc2e34a260774f32a50203010001";


	public ServerInterface() {}
	    

	@Override
	public CipheredMessage register(String source, CipheredMessage msg) throws RemoteException {

		CryptoManager manager = new CryptoManager(SERVER_KEY, "passwd");
		//decipher
		Message decipheredMessage = manager.decipherCipheredMessage(msg, source);

		System.out.println(msg);

		Message message = new Message(true);
		CipheredMessage cipheredMessage = manager.makeCipheredMessage(message, source);
		return cipheredMessage;
	}

	@Override
		public void send(PublicKey source, PublicKey destination, int amount) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public String check(PublicKey source) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String receive(PublicKey source) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String audit(PublicKey source) {
			// TODO Auto-generated method stub
			return null;
		}
	
}
