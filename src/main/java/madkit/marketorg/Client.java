/*
 * Copyright 1997-2012 Fabien Michel, Olivier Gutknecht, Jacques Ferber
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
import madkit.kernel.Madkit;
import madkit.kernel.Madkit.Option;
import madkit.kernel.Message;
import madkit.marketorg.model.Offer;
import madkit.marketorg.model.Request;
import madkit.marketorg.negociation.RequestNegociationInterface;
import madkit.marketorg.negociation.StaticRequestNegociation;
import madkit.message.ObjectMessage;
import madkit.message.StringMessage;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Fabien Michel, Olivier Gutknecht, Jacques Ferber
 * @version 5.1
 */
@SuppressWarnings("serial")
public class Client extends Agent {

    static int nbOfClientsOnScreen = 0;

    private JPanel blinkPanel;
    private static ImageIcon clientImage = new ImageIcon(new ImageIcon(Client.class.getResource("images/client.png")).getImage().getScaledInstance(70, 70, Image.SCALE_SMOOTH));
    private final String product = Provider.availableTransports.get((int) (Math.random() * Provider.availableTransports.size()));

	private final List<String> preferedCompanies = getRandomCompanies(product, null);
	private final List<String> blakedCompanies = getRandomCompanies(product, preferedCompanies);
	private final Integer budgetMax = (int)(200+Math.random()*300);
	private final Date limitDate = new Date();

	private Request lastRequest;

	private int nbTry;
	private RequestNegociationInterface negociation;

	public static List<String> getRandomCompanies(String type, List<String> blaked) {
		List<String> companies = Provider.availableCompanies.get(type);
		List<String> choosedCompanies = new ArrayList<String>();

		if(companies != null)
		{
			double ratio = Math.random();

			for (String company : companies) {
				if (Math.random() > ratio && blaked != null && !blaked.contains(company))
					choosedCompanies.add(company);
			}
		}

		return choosedCompanies;
	}


	@Override
    protected void activate() {
		createGroupIfAbsent(MarketOrganization.COMMUNITY, MarketOrganization.CLIENT_GROUP, true, null);
		requestRole(MarketOrganization.COMMUNITY, MarketOrganization.CLIENT_GROUP, MarketOrganization.CLIENT_ROLE, null);
		int pause = 1000 + (int) (Math.random() * 2000);
		getLogger().info(() -> "I will be looking for a " + product + " in " + pause + " ms !");
		nbTry = 0;
		negociation = new StaticRequestNegociation();
		pause(pause);
    }

    @Override
    protected void live() {
		boolean haveTicket = false;
		while (!haveTicket) {
			Message brokerAnswer = null;
			lastRequest = new Request(product, preferedCompanies, blakedCompanies, budgetMax, limitDate);
			while (brokerAnswer == null) {
				brokerAnswer = sendMessageWithRoleAndWaitForReply(
					MarketOrganization.COMMUNITY,
					MarketOrganization.CLIENT_GROUP,
					MarketOrganization.BROKER_ROLE,
					new ObjectMessage<>(lastRequest),
					MarketOrganization.CLIENT_ROLE,
					1000
					);
				getLogger().info(() -> "For now there is nothing for me :(");
				nbTry++;

				if(nbTry > 6)
				{
					return;
				}

				pause(500);
			}
			logFindBroker(brokerAnswer);// I found a broker and he has something for me
			haveTicket = beginNegociation((StringMessage) brokerAnswer);
		}
    }

	@Override
    protected void end() {
		getLogger().info(() -> "I will quit soon now, buit I will launch another one like me !");
		pause((int) (Math.random() * 2000 + 500));
		launchAgent(new Client(), 4, true);
    }

    private void logFindBroker(Message brokerAnswer) {
		getLogger().info(() -> "I found a broker : " + brokerAnswer.getSender());
		if (blinkPanel != null) {
			blinkPanel.setBackground(Color.YELLOW);
			pause(1000);
		}
    }

	private boolean beginNegociation(StringMessage brokerAnswer) {
		String contractGroupID = brokerAnswer.getContent();
		requestRole(MarketOrganization.COMMUNITY, contractGroupID, MarketOrganization.CLIENT_ROLE);

		ObjectMessage<Offer> providerOffer = null;
		int nbRequest = 0;
		while (providerOffer == null && nbRequest < 6)
		{
			List<Message> bids = askForOffers(contractGroupID, lastRequest);
			ObjectMessage<Offer> selectedOffer = selectBid(bids);
			if(selectedOffer != null)
			{
				providerOffer = selectedOffer;
			}
			else
			{
				updateRequest(bids);
			}
			nbRequest++;
		}

		if(providerOffer != null)
		{
			boolean ticket = buyTicket(providerOffer);
			leaveNegociation(contractGroupID);
			return ticket;
		}
		else
		{
			getLogger().info(() -> "End of negociation with fail, nbr try reached 6");
		}

		leaveNegociation(contractGroupID);
		return false;

	}

	private void leaveNegociation(String contractGroupID) {
		broadcastMessageWithRole(// wait all answers
				MarketOrganization.COMMUNITY, // target community
				contractGroupID, // target group
				MarketOrganization.PROVIDER_ROLE, // target role
				new StringMessage("leave"), // say i'm leaving negociation
				MarketOrganization.CLIENT_ROLE); // I am a client
	}

	private ObjectMessage<Offer> selectBid(List<Message> bids) {
		ObjectMessage<Offer> best = null;

		for (Message m : bids) {
			if(m instanceof ObjectMessage && ((ObjectMessage)m).getContent() instanceof Offer) {
				ObjectMessage<Offer> offerMessage = ((ObjectMessage<Offer>) m);
				Offer offer = offerMessage.getContent();

//				if(!request.getBlackedCompanies().contains(offer.getProvider()))
//				{
					best = offerMessage;
//				}
			}
			else
			{
				getLogger().info(() -> "Received not offer message from " + m.getSender());
			}
		}

		return best;
	}

	private void updateRequest(List<Message> bids)
	{
		if(lastRequest == null)
		{
			lastRequest = negociation.getNewRequest(this);
		}
		else
		{
			lastRequest = negociation.updateNewRequest(this, bids);
		}
	}

	private List<Message> askForOffers(String contractGroupID, Request request) {
		List<Message> bids = broadcastMessageWithRoleAndWaitForReplies(// wait all answers
				MarketOrganization.COMMUNITY, // target community
				contractGroupID, // target group
				MarketOrganization.PROVIDER_ROLE, // target role
				new ObjectMessage<>(request), // ask for offers
				MarketOrganization.CLIENT_ROLE, // I am a broker: Let the receiver know about it
				900); // I cannot wait the end of the universe
		return bids;
	}

	private boolean buyTicket(ObjectMessage<Offer> providerAnswer) {
		getLogger().info("Try buy ticket from : " + providerAnswer.getSender());
		Message ticket = sendReplyAndWaitForReply(providerAnswer, new StringMessage("money"), 1000);

		if (ticket != null) {
			getLogger().info("Yeeeaah: I have my ticket :) ");
			if (hasGUI()) {
			blinkPanel.setBackground(Color.GREEN);
			}
			leaveGroup(MarketOrganization.COMMUNITY, MarketOrganization.CLIENT_GROUP);
			pause((int) (1000 + Math.random() * 2000));
			return true;
		}
		else
		{
			getLogger().info("Waiting too long, cancel buy ticket from " + providerAnswer.getSender());
		}
		return false;
    }

    @Override
    public void setupFrame(AgentFrame frame) {
		JPanel p = new JPanel(new BorderLayout());
		// customizing but still using the OutputPanel from MaDKit GUI
		p.add(new OutputPanel(this), BorderLayout.CENTER);
		blinkPanel = new JPanel();
		blinkPanel.add(new JLabel(clientImage));
		blinkPanel.add(new JLabel(new ImageIcon(getClass().getResource("images/" + product + ".png"))));
		p.add(blinkPanel, BorderLayout.NORTH);
		p.validate();
		frame.add(p);
		int xLocation = nbOfClientsOnScreen++ * 390;
		if (xLocation + 390 > Toolkit.getDefaultToolkit().getScreenSize().getWidth())
			nbOfClientsOnScreen = 0;
		frame.setLocation(xLocation, 0);
		frame.setSize(390, 300);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
		nbOfClientsOnScreen = 0;
		Broker.nbOfBrokersOnScreen = 0;
		new Madkit(Option.launchAgents.toString(), Broker.class.getName() + ",true,3;" + Client.class.getName() + ",true,2;" + Provider.class.getName() + ",true,7");
    }

	public String getProduct() {
		return product;
	}

	public List<String> getPreferedCompanies() {
		return preferedCompanies;
	}

	public List<String> getBlakedCompanies() {
		return blakedCompanies;
	}

	public Integer getBudgetMax() {
		return budgetMax;
	}

	public Date getLimitDate() {
		return limitDate;
	}

	public Request getLastRequest() {
		return lastRequest;
	}
}