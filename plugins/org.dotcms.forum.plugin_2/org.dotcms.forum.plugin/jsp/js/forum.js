function setSubmitContentForm() {
  if(dojo.byId("submitContentForm")){
    dojo.byId("submitContentForm").setAttribute("action", "/dotCMS/submitForumContent");
  }
}
  
function editSubscription() {
  var userId = document.getElementById("userId").value;
  var contentIdentifier=document.getElementById("contentIdentifier").value;
  if(userId == 'null' || userId == '') {
  
    var divCollection = document.getElementsByTagName("div");
        for (var i=0; i<divCollection.length; i++) {
            if(divCollection[i].getAttribute("class") == "formBody") {
                divCollection[i].innerHTML = "Please <a href=\"/dotCMS/login\">login</a> to subscribe to this Content.";
            } 
        }
    
    if(dojo.byId("addReply")){
      $('#addReply').show('normal', function() {
      window.location.href="#addThread";
      setSubmitContentForm();
      });
    }

    else if(dojo.byId("addThread")){
      $('#addThread').show('normal', function() {
      window.location.href="#addThread";
      setSubmitContentForm();
      });
    }
  }
  else {
    
    ForumAjax.isUserSubscribed(userId, contentIdentifier, {
          callback:function(result) { 
            if(result==true) {
              ForumAjax.unsubscribeToForumContent(userId, contentIdentifier, unsubscribeToForumContentCallback);
            } else if(result==false) { 
              ForumAjax.subscribeToForumContent(userId, contentIdentifier, subscribeToForumContentCallback)
            } 
            
          }  
        });
  
  }
  
}

function subscribeToForumContentCallback (data) {
  if(data["subscribeError"] != null ) {
    alert("There was an error with your subscription");
  }else{
    document.getElementById("subscribeButton").innerHTML = 'Unsubscribe';
    alert("You are subscribed to this content");
  }
}

function unsubscribeToForumContentCallback (data) {
  if(data["subscribeError"] != null ) {
    alert("There was an error with your subscription");
  }else{
    document.getElementById("subscribeButton").innerHTML = 'Subscribe';
    alert("You are unsubscribed to this content");
  }
}

function setLastModifiedFieldValue() {
  var dateValue = document.getElementById("lastModified").value;
   if(dateValue != 'null' || dateValue != '') {
   var d=new Date();
   var currentYear = d.getFullYear();
   var currentMonth= d.getMonth() + 1;
   var currentDate= d.getDate();
   var currentHours = d.getHours();
   var currentMinutes = d.getMinutes();
   
   if (currentMonth < 10){
   currentMonth = '0' + currentMonth.toString();
   }
   
   if (currentDate < 10){
   currentDate = '0' + currentDate.toString();
   }
   
   if (currentHours < 10){
   currentHours = '0' + currentHours.toString();
   }
   
   if (currentMinutes  < 10){
   currentMinutes  = '0' + currentMinutes.toString();
   }
   
   var LocalDate = currentYear  + "-" + currentMonth + "-" + currentDate + " " + currentHours + ":" + currentMinutes ;
   document.getElementById("lastModified").value = LocalDate;
  
  }


}