package uk.ac.lancs.e_science.profile2.tool.pages.panels;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.ContextImage;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

import uk.ac.lancs.e_science.profile2.api.Profile;
import uk.ac.lancs.e_science.profile2.api.ProfileImageManager;
import uk.ac.lancs.e_science.profile2.api.SakaiProxy;
import uk.ac.lancs.e_science.profile2.tool.ProfileApplication;
import uk.ac.lancs.e_science.profile2.tool.components.ProfileImageRenderer;
import uk.ac.lancs.e_science.profile2.tool.dataproviders.ConfirmedFriendsDataProvider;
import uk.ac.lancs.e_science.profile2.tool.models.FriendAction;
import uk.ac.lancs.e_science.profile2.tool.pages.ViewProfile;
import uk.ac.lancs.e_science.profile2.tool.pages.windows.RemoveFriend;

public class ConfirmedFriends extends Panel {
	
	private static final long serialVersionUID = 1L;
	private transient SakaiProxy sakaiProxy;
	private transient Profile profile; 
	private int numConfirmedFriends = 0;
	private boolean updateable = false;
	
	public ConfirmedFriends(final String id, final String userX) {
		super(id);
		
		//get SakaiProxy
		sakaiProxy = ProfileApplication.get().getSakaiProxy();
		
		//get Profile
		profile = ProfileApplication.get().getProfile();
			
		//setup model to store the actions in the modal windows
		final FriendAction friendActionModel = new FriendAction();
		
		//get id of user viewing this page (will be the same if user is viewing own list, different if viewing someone else's)
		final String userY = sakaiProxy.getCurrentUserId();
		
		//if viewing own friends, you can manage them.
		if(userX.equals(userY)) {
			updateable = true;
		}
		
		//get our list of confirmed friends as an IDataProvider
		ConfirmedFriendsDataProvider provider = new ConfirmedFriendsDataProvider(userX);
		
		//init number of friends
		numConfirmedFriends = provider.size();
		
		//model so we can update the number of friends
		IModel numConfirmedFriendsModel = new Model() {
			private static final long serialVersionUID = 1L;

			public Object getObject() {
				return numConfirmedFriends;
			} 
		};
		
		//heading
		final WebMarkupContainer confirmedFriendsHeading = new WebMarkupContainer("confirmedFriendsHeading");
		Label confirmedFriendsLabel = new Label("confirmedFriendsLabel");
		//if viewing own list, "my friends", else, "their name's friends"
		if(updateable) {
			confirmedFriendsLabel.setModel(new ResourceModel("heading.friends.my"));
		} else {
			String displayName = sakaiProxy.getUserDisplayName(userX);
			confirmedFriendsLabel.setModel(new StringResourceModel("heading.friends.view", null, new Object[]{ displayName } ));
		}
		confirmedFriendsHeading.add(confirmedFriendsLabel);
		confirmedFriendsHeading.add(new Label("confirmedFriendsNumber", numConfirmedFriendsModel));
		confirmedFriendsHeading.setOutputMarkupId(true);
		add(confirmedFriendsHeading);
		
		//no friends message (only show if viewing own list)
		/*
		final WebMarkupContainer noFriendsContainer = new WebMarkupContainer("noFriendsContainer");
		noFriendsContainer.setOutputMarkupId(true);
		
		final Link noFriendsLink = new Link("noFriendsLink", new ResourceModel("link.friend.search")) {
			private static final long serialVersionUID = 1L;

			public void onClick() {
				setResponsePage(new MySearch());
			}

		};
		noFriendsContainer.add(noFriendsLink);
		noFriendsContainer.setVisible(false);
		add(noFriendsContainer);
		*/
		
	
		
		//container which wraps list
		final WebMarkupContainer confirmedFriendsContainer = new WebMarkupContainer("confirmedFriendsContainer");
		confirmedFriendsContainer.setOutputMarkupId(true);
		
		//results
		DataView confirmedFriendsDataView = new DataView("results-list", provider) {
			private static final long serialVersionUID = 1L;

			protected void populateItem(final Item item) {
		        
		    	//get friendId
		    	final String friendId = (String)item.getModelObject();
		    			    	
		    	//setup values
		    	String displayName = sakaiProxy.getUserDisplayName(friendId);
		    	boolean friend;
		    	
		    	//get friend status
		    	if(updateable) {
		    		friend = true; //viewing own list of confirmed fiends so must be a friend
		    	} else {
		    		friend = profile.isUserXFriendOfUserY(userY, friendId); //other person viewing, check if they are friends
		    	}
	    		
		    	//is profile image allowed to be viewed by this user/friend?
				final boolean isProfileImageAllowed = profile.isUserXProfileImageVisibleByUserY(friendId, userY, friend);
				
				//image
				item.add(new ProfileImageRenderer("result-photo", friendId, isProfileImageAllowed, ProfileImageManager.PROFILE_IMAGE_THUMBNAIL, true));
		    	
		    	//name and link to profile
		    	Link profileLink = new Link("result-profileLink") {
					private static final long serialVersionUID = 1L;

					public void onClick() {
						setResponsePage(new ViewProfile(friendId));
					}
					
				};
				profileLink.add(new Label("result-name", displayName));
		    	item.add(profileLink);
		    	
		    	/* ACTIONS */
		    	
		    	//REMOVE FRIEND MODAL WINDOW
				final ModalWindow removeFriendWindow = new ModalWindow("removeFriendWindow");
				removeFriendWindow.setContent(new RemoveFriend(removeFriendWindow.getContentId(), removeFriendWindow, friendActionModel, userX, friendId)); 
				
				//REMOVE FRIEND LINK
		    	final AjaxLink removeFriendLink = new AjaxLink("removeFriendLink") {
					private static final long serialVersionUID = 1L;
					public void onClick(AjaxRequestTarget target) {
						
						//target.appendJavascript("Wicket.Window.get().window.style.width='800px';");
						removeFriendWindow.show(target);
						target.appendJavascript("fixWindowVertical();"); 

					}
				};
				ContextImage removeFriendIcon = new ContextImage("removeFriendIcon",new Model(ProfileImageManager.DELETE_IMG));
				removeFriendLink.add(removeFriendIcon);
				removeFriendLink.add(new AttributeModifier("title", true,new ResourceModel("link.title.removefriend")));
				item.add(removeFriendLink);
				
				//can only delete if own friends
				if(!updateable) {
					removeFriendLink.setEnabled(true);
					removeFriendLink.setVisible(false);
				}
				
				// REMOVE FRIEND MODAL WINDOW HANDLER 
				removeFriendWindow.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
					private static final long serialVersionUID = 1L;
					public void onClose(AjaxRequestTarget target){
		            	if(friendActionModel.isRemoved()) { 
		            		
		            		//decrement number of friends
		            		numConfirmedFriends--;
		            		
		            		//remove friend item from display
		            		target.appendJavascript("$('#" + item.getMarkupId() + "').slideUp();");
		            		
		            		//update label
		            		target.addComponent(confirmedFriendsHeading);
		            		
		            		//if none left, hide whole thing
		            		if(numConfirmedFriends==0) {
		            			target.appendJavascript("$('#" + confirmedFriendsContainer.getMarkupId() + "').fadeOut();");
		            			
		            			/*
		            			if(userX.equals(userY)) {
		            				noFriendsContainer.setVisible(true);
		            				target.addComponent(noFriendsContainer);
		            			}
		            			*/
		            			
		            		}
		            		
		            	}
		            }
		        });
				item.add(removeFriendWindow);
				
				item.setOutputMarkupId(true);
				
		    }
			
		};
		confirmedFriendsDataView.setOutputMarkupId(true);
		confirmedFriendsContainer.add(confirmedFriendsDataView);

		//add results container
		add(confirmedFriendsContainer);
		
		
		
		
		//initially, if no friends, hide container
		if(numConfirmedFriends == 0) {
			confirmedFriendsContainer.setVisible(false);
			
		}
		
		
		
		

	}
	
}
