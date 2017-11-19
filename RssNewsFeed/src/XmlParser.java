import static org.w3c.dom.Node.ELEMENT_NODE;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

abstract class XmlFunctions implements IXmlFunctions{
	protected Map<Integer,CategoryDetails> cList=null;
	
	public abstract void parseDOM();
	public abstract void getXML();
	public abstract void makeURL();
	
	//Empty implementations
	public void setCatList(Map<Integer,CategoryDetails> c) {}
	public void setCategory(int c) {}
}

public class XmlParser extends XmlFunctions {
	
	private final String XMLPARSER = this.getClass().getSimpleName();
	private final String SORRY_INFO="########Sorry, No details found, please check the link ######";
	private final String SORRY_LINK="########Sorry, No link found please go to https://www.cbsnews.com/ ######";
	
	private ItemsListDB itemsDB;
	private int cat;
	private String url;
	private String fileName;
	
	public XmlParser() {
		url = RssCategoryNews.CBS_RSS_URL;
		fileName = null;
	}
	
	@Override
	public void setCatList(Map<Integer,CategoryDetails> c) {
		System.out.println(XMLPARSER+".setCatList():called");
		cList = c;
	}
	
	@Override
	public void setCategory(int c) {
		System.out.println(XMLPARSER+".setCategory():called");
		cat = c;
	}
	
	@Override
	public void makeURL() {
		System.out.println(XMLPARSER+".makeURL():called");
		
		System.out.println(XMLPARSER+".makeURL():Extract category string");
		String catVal = cList.get(cat).getCategory();
		System.out.println(XMLPARSER+".makeURL():User selected:"+ catVal);
		
		System.out.println(XMLPARSER+".makeURL():Extract filename");
		fileName = cList.get(cat).getfName();
		
		System.out.println(XMLPARSER+".makeURL():create items DB list");
		itemsDB = cList.get(cat).getItemsDB();
		
		System.out.println(XMLPARSER+".makeURL():set url");
		url = RssCategoryNews.CBS_RSS_URL;
			
		if (url.contains("category")) {
			url = url.replace("category", catVal);
			System.out.println(XMLPARSER+".makeUrl():url replaced by category+"+url);
		}
		
		System.out.println(XMLPARSER+".makeURL():create items DB list");
		itemsDB.createList();
	}//end makeUrl()
	
	@Override
	public void getXML() {
		System.out.println(XMLPARSER+".getXML():called");
		
		HttpHandler sh = new HttpHandler();
		// Making a request to url and getting response
		
		try {
			sh.setFileName(fileName);
			sh.makeServiceCall(url);
			}catch(IOException ie){
				System.out.println(ie.getMessage());
				System.exit(0);
			}
	}//end getXML()
	
	@Override
	public void parseDOM() {
		System.out.println(XMLPARSER+".parseDOM():called");
		//Get the DOM Builder Factory
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	
		try {
			//Get DOM builder
			DocumentBuilder builder = factory.newDocumentBuilder();
			//Load and parser XML, document contains the entire XML as a tree
			System.out.println(XMLPARSER+".parseDOM():set filename="+fileName);
			
			Document document = builder.parse(new File(fileName));
			//Normalize the XML Structure; It's just too important !!
			document.getDocumentElement().normalize();
			
			//Here comes the root node"
			Element root = (Element) document.getDocumentElement();
			NodeList nList = root.getChildNodes();
			for(int i=0; i<nList.getLength(); i++) {
				Node n = nList.item(i);
				if (n.getNodeType() == ELEMENT_NODE) {
					System.out.println(XMLPARSER+".parseDOM():Element["+i+"]="+n.getNodeName());
					if (n.getNodeName().equals("channel")) {
						extractChannelChildItems(n);
					}//end if
				}//end if
		    }//end for			
		}catch(ParserConfigurationException pce) {
			pce.printStackTrace();
			System.exit(0);
		}catch ( IOException ie) {
			ie.printStackTrace();
			System.exit(0);
		}catch( SAXException saxe) {
			saxe.printStackTrace();
			System.exit(0);
		}
	}//end parseDOM()
	
	
	/*************************************************************************************
	 *                                 PRIVATE FUNCTIONS                                 *
	 ************************************************************************************/                              
	private void extractAllItems(Node item) {
		System.out.println(XMLPARSER+".extractAllItems():called");
		
		NodeList iList = item.getChildNodes();
		int iLength = iList.getLength();
		System.out.println(XMLPARSER+".extractAllItems():iLength="+iLength);

		for(int i=0; i<iLength;i++) {
			String titleVal, linkVal, descVal;
			boolean instanceFound=false;
			Node cNode = iList.item(i);
			titleVal = linkVal = descVal = null;
			if (cNode == null) {
				//If no node present continue to next item
				continue;
			}
			if (cNode instanceof Element) {
				//String content = cNode.getLastChild().getTextContent().trim();
			
				switch(cNode.getNodeName()) {		
				case "title":{
					System.out.println(XMLPARSER+".extractAllItems():title found");
					instanceFound = true;
					Node lastChild = cNode.getLastChild();
					//check if last child is present(text)
					if (lastChild == null) {
						titleVal = SORRY_INFO;
					}
					else {
						titleVal = lastChild.getTextContent().trim();
					}
				}
				break;
				
				case "link":{
					System.out.println(XMLPARSER+".extractAllItems():link found");
					instanceFound = true;
					Node lastChild = cNode.getLastChild();
					if (lastChild == null) {
						linkVal = SORRY_LINK;
					}
					else {
						linkVal = lastChild.getTextContent().trim();
					}
				}
				break;
				
				case "description":{
					System.out.println(XMLPARSER+".extractAllItems():description found");
					instanceFound = true;
					Node lastChild = cNode.getLastChild();
					if (lastChild == null) {
						descVal = SORRY_INFO;
					}
					else {
						descVal = lastChild.getTextContent().trim();
					}
				}
				break;
				
				default: 
					break;
				}
				
				if ( instanceFound ) {
					//if element found then add it to the list
					NewsItem newsch = itemsDB.getNewsItem();
					newsch.setTitle(titleVal);
					newsch.setLink(linkVal);
					newsch.setDescription(descVal);
					itemsDB.addToList(newsch);
					instanceFound = false;
				}			
			}//end switch
		}// end of if(cNode instanceof Element)
	}//extractAllItems()
	
	private void extractChannelChildItems(Node node) {
		System.out.println(XMLPARSER+".extractChannelChildItems():called");
		
		NodeList nList = node.getChildNodes();
		int nLength = nList.getLength();
		System.out.println(XMLPARSER+".extractChannelChildItems():nLength="+nLength);
		for(int i=0; i<nLength;i++) {
			Node item = nList.item(i);
			if ((item instanceof Element) && item.getNodeName().equals("item")) {
				extractAllItems(item);
			}//end if
		}// end for
	}//end of extractChannelChildItems()
}
