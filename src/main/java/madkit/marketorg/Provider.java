/*
 * Copyright 1997-2013 Fabien Michel, Olivier Gutknecht, Jacques Ferber
 * 
 * This file is part of MaDKit_Demos.
 * 
 * MaDKit_Demos is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MaDKit_Demos is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MaDKit_Demos. If not, see <http://www.gnu.org/licenses/>.
 */
package madkit.marketorg;

import madkit.gui.AgentFrame;
import madkit.gui.OutputPanel;
import madkit.kernel.Agent;
import madkit.kernel.Message;
import madkit.marketorg.model.Offer;
import madkit.marketorg.model.Request;
import madkit.marketorg.negociation.NegociationInterface;
import madkit.marketorg.negociation.StaticNegociation;
import madkit.message.ObjectMessage;
import madkit.message.StringMessage;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author Fabien Michel, Olivier Gutknecht, Jacques Ferber
 * @version 5.1
 */
public class Provider extends Agent {

    /**
     * 
     */
    private static final long serialVersionUID = 9125493540160734521L;

    public static List<String> availableTransports = Arrays.asList("train", "boat", "plane", "bus");

	public static final Map<String, List<String>> availableCompanies;
	static {
		Map<String, List<String>> aMap = new HashMap<>();
		aMap.put("train", Arrays.asList("SNCF", "ROJA"));
		aMap.put("boat", Arrays.asList("CORSICA", "TITANIC"));
		aMap.put("plane", Arrays.asList("AIR FRANCE", "EASY JET"));
		aMap.put("bus", Arrays.asList("FLIX", "OUIGO"));
		availableCompanies = Collections.unmodifiableMap(aMap);
	}

    final private static Map<String, ImageIcon> icons = new HashMap<>();

    static {
		for (String competence : availableTransports) {
			icons.put(competence, new ImageIcon(Provider.class.getResource("images/" + competence + ".png")));
		}
    }

    private static int nbOfProvidersOnScreen = 0;

    private String competence;
    private JPanel blinkPanel;
    private String name;

    private Offer lastOffer;

    private NegociationInterface negociation;

    public Provider() {
		competence = Provider.availableTransports.get((int) (Math.random() * Provider.availableTransports.size()));
		name = Provider.availableCompanies.get(competence).get((int) (Math.random() * Provider.availableCompanies.get(competence).size()));
		negociation = new StaticNegociation();
    }

    public void activate() {
		createGroupIfAbsent(MarketOrganization.COMMUNITY, MarketOrganization.PROVIDERS_GROUP, true, null);
		requestRole(MarketOrganization.COMMUNITY, MarketOrganization.PROVIDERS_GROUP, competence + "-" + MarketOrganization.PROVIDER_ROLE, null);
    }

    public void live() {
		while (true) {
			Message m = waitNextMessage();
			if (m.getSender().getRole().equals(MarketOrganization.BROKER_ROLE))
				handleBrokerMessage(m);
			else if(m.getSender().getRole().equals(MarketOrganization.CLIENT_ROLE))
				handleClientMessage(m);
		}
    }

    private void handleClientMessage(Message m) {
		if (m instanceof StringMessage) {
			handleClientStringMessage((StringMessage)m);
		}
		else if(m instanceof ObjectMessage && ((ObjectMessage) m).getContent() instanceof Request)
		{
			ObjectMessage messageRequest = (ObjectMessage) m;
			//Request request = (Request)messageRequest.getContent();
			negociate((ObjectMessage<Request>) m);
		}
		else {
			getLogger().info("nothing to do here");
		}
    }

	private void negociate(ObjectMessage<Request> m) {
		getLogger().info("I received a call for a new offer from " + m.getSender());
		updateOffer(m.getContent());
		sendReply(m, new ObjectMessage<>(lastOffer));
	}

	private void updateOffer(Request request)
	{
		if(lastOffer == null || request == null)
		{
			lastOffer = negociation.getNewOffer(competence, name);
		}
		else
		{
			lastOffer = negociation.updateNewOffer(request, lastOffer, competence, name);
		}
	}

	private void handleClientStringMessage(StringMessage m) {
		if (m.getContent().equals("i-choose-you")) {
			getLogger().info("I have been selected to sell a ticket :)");
			sendReply(m, new Message()); // just an acknowledgment
		}
		else if (m.getContent().equals("money")) {
			finalizeContract(m);
		}
		else if (m.getContent().equals("leave")) {
			leaveNegociation(m);
		}
	}

	private void handleBrokerMessage(Message m) {
		if (m instanceof StringMessage) {
			handleBrokerStringMessage((StringMessage)m);
		}
		else
		{
			getLogger().info("nothing to do here");
		}
    }

	private void handleBrokerStringMessage(StringMessage m) {
		if (m.getContent().equals("make-bid-please")) {
			getLogger().info("I received a call for bid from " + m.getSender());
			updateOffer(null);
			sendReply(m, new ObjectMessage<>(lastOffer));
		}
		else {
			iHaveBeenSelectedForNegociation(m);
		}
	}

    private void iHaveBeenSelectedForNegociation(StringMessage m) {
		if (hasGUI()) {
			blinkPanel.setBackground(Color.YELLOW);
		}
		getLogger().info("I have been selected for negociation :)");
		String contractGroup = m.getContent();
		createGroupIfAbsent(MarketOrganization.COMMUNITY, contractGroup, true);
		requestRole(MarketOrganization.COMMUNITY, contractGroup, MarketOrganization.PROVIDER_ROLE);
		sendReply(m, new Message()); // just an acknowledgment
    }

    private void finalizeContract(StringMessage m) {
		if (hasGUI()) {
			blinkPanel.setBackground(Color.GREEN);
		}
		getLogger().info("I have sold something: That's great !");
		sendReply(m, new StringMessage("ticket"));
		leaveGroup(MarketOrganization.COMMUNITY, m.getSender().getGroup());
		pause((int) (Math.random() * 2000 + 1000));// let us celebrate !!
		if (hasGUI()) {
			blinkPanel.setBackground(Color.LIGHT_GRAY);
		}
    }

    private void leaveNegociation(StringMessage m) {
		if (hasGUI()) {
			blinkPanel.setBackground(Color.RED);
		}
		getLogger().info("I haven't sold anything and i leave this negociation room");
		pause((int) (Math.random() * 2000 + 1000));// let us celebrate !!
		leaveGroup(MarketOrganization.COMMUNITY, m.getSender().getGroup());
		if (hasGUI()) {
			blinkPanel.setBackground(Color.LIGHT_GRAY);
		}
    }

    @Override
    public void setupFrame(AgentFrame frame) {
		JPanel p = new JPanel(new BorderLayout());
		// customizing but still using the madkit.gui.OutputPanel.OutputPanel
		p.add(new OutputPanel(this), BorderLayout.CENTER);
		blinkPanel = new JPanel();
		blinkPanel.add(new JLabel(name));
		blinkPanel.add(new JLabel(icons.get(competence)));
		p.add(blinkPanel, BorderLayout.NORTH);
		blinkPanel.setBackground(Color.LIGHT_GRAY);
		p.validate();
		frame.add(p);
		int xLocation = nbOfProvidersOnScreen++ * 300;
		if (xLocation + 300 > Toolkit.getDefaultToolkit().getScreenSize().getWidth())
			nbOfProvidersOnScreen = 0;
		frame.setLocation(xLocation, 640);
		frame.setSize(300, 300);
    }

}
