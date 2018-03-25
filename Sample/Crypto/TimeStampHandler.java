package org.komparator.security.handler;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.soap.SOAPFaultException;

import org.w3c.dom.DOMException;

public class TimeStampHandler implements SOAPHandler<SOAPMessageContext>{
	
	private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

	@Override
	public boolean handleMessage(SOAPMessageContext smc) {
		try {
			checkTime(smc);
		} catch (SOAPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public boolean handleFault(SOAPMessageContext smc) {
		try {
			smc.getMessage().writeTo(System.out);
			System.out.println("Erro: mensagem demorou mais de 3 segundos");
		} catch (SOAPException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

	private void checkTime(SOAPMessageContext smc) throws SOAPException, IOException, DOMException, ParseException, RuntimeException {
		Boolean outbound = (Boolean) smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
		Date date = new Date();
		String date1 = dateFormatter.format(date);
		if(outbound){
			SOAPHeader soapHeader = smc.getMessage().getSOAPHeader();
            if(soapHeader == null) {
                soapHeader = smc.getMessage().getSOAPPart().getEnvelope().addHeader();
            }
            Name name = smc.getMessage().getSOAPPart().getEnvelope().createName("TimeHeader", "th","http://org.su");
            SOAPElement element = soapHeader.addChildElement(name);
            element.addTextNode(date1);
            smc.getMessage().writeTo(System.out);
		}
		
	}

	@Override
	public void close(MessageContext context) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<QName> getHeaders() {
		// TODO Auto-generated method stub
		return null;
	}

}
