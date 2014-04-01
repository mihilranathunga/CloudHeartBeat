/*
 * Copyright 2005-2012 WSO2, Inc. http://www.wso2.org
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

var registerTableEvents= function(){


    $('.parent-row a.expandingLink').click(function(){
        var nextOneIsAChild = true;
        var currentState = 'close'; // close is the state when all the child-row elems are hidden
        var next = $(this).parent().parent().next();
        if($('i',this).hasClass('open')){
            currentState = 'open';
            $('i',this).removeClass('open');
            $('i',this).addClass('close');
        }else{
            $('i',this).removeClass('close');
            $('i',this).addClass('open');
        }

        while(nextOneIsAChild){
            if(next.hasClass('parent-row') || next[0].nodeName==undefined){
                nextOneIsAChild = false;
            }else{
                if(currentState == 'open'){
                    next.hide();
                }else{
                    next.show();
                }
            }
            next = next.next();
        }
    });


    $('.toggle-link').hover(function(){
        $(this).next().show('fast').mouseleave(function(){
            $(this).hide();
        });
    });


    $('#full-screen-icon').click(function(){


        var width = $(document).width();
        var height= $(document).height();

        window.scrollTo(0,0);
        if(!onFullScreen){
            $('#mainTable').clone().appendTo('#full-screen');
            $('#full-screen div.hiddenLogo').show();
            $('#full-screen div#mainTable').id = 'mainTable-fs';
            $('#full-screen div#status').id = 'status-fs';
            $('#full-screen a.showNotesLink').removeAttr('onclick').css('cursor','default');
            $('#full-screen a.expandingLink').css('cursor','default');
            $('#full-screen i').remove();
            $('#full-screen tr.child-row').remove();
            //$('#full-screen div#mainTable').css('width','100%');
            $('#full-screen a#full-screen-icon').removeAttr('id').click(function(){
                $('#full-screen').empty();
                $('#full-screen').hide();
                $("body").css("overflow","auto");
                $('#full-screen div.hiddenLogo').hide();

            });

            $('#full-screen a.toggle-link').hover(function(){
                var pos=$(this).position();
                var left = pos.left;
                var top = pos.top;
                $(this).next().css('left',left+'px');

                $(this).next().show('fast').mouseleave(function(){
                    $(this).hide();
                });
            });
            $('#full-screen').css('width',width+"px");
            $('#full-screen').css('height',height+"px").show();

            var top=0;
            if($('div#full-screen #mainTable').height() > $(window).height()){
                $("body").css("overflow","auto");
            }else{
                $("body").css("overflow","hidden");
                top = ($(window).height() - $('div#full-screen #mainTable').height())/2;
                if(top+$('div#full-screen #mainTable').height()>$(window).height()){
                    top=0;
                }
            }
            $('div#full-screen #mainTable').css('margin-top',top+'px');
            $('#full-screen').css('width', $(document).width()+"px");
            onFullScreen = true;

        }else{
            $('#full-screen').empty();
            $('#full-screen').hide();
            $("body").css("overflow","auto");
            $('#full-screen div.hiddenLogo').hide();
            onFullScreen =false;
        }

    });

    if(onFullScreen){
        $('#full-screen i').remove();
        $('#full-screen a.expandingLink').unbind('click');

    }
}


var numberOfSlides;
var currentSlide = 1;
var slideItems = function() {
    numberOfSlides = $('.status-table-state').length
    var index = parseInt($(this).attr('data-value'));
    if ($(this).hasClass('slide-right')) {
        index++;
    } else {
        index--;
    }
    if (index > numberOfSlides) {
        index = 1;
    }
    if (index < 1) {
        index = numberOfSlides;
    }
    if (index == numberOfSlides) {
        $('.slide-left').css('visibility','');
        $('.slide-right').css('visibility','hidden');
    } else if (index == 1) {
        $('.slide-left').css('visibility','hidden');
        $('.slide-right').css('visibility','');
    } else {
        $('.slide-left').css('visibility','');
        $('.slide-right').css('visibility','');
    }

    $('.slide-left').attr('data-value', index);
    $('.slide-right').attr('data-value', index);

    goto(index);


};
var registerElementsHistory = function(){
    $('.slide-left').click(slideItems);
    $('.slide-right').click(slideItems);
};
$(document).ready(function() {
    registerElementsHistory();
});

function goto(index, t) {
    //animate to the div id.
    $(".status-table-wrapper-content").delay(100).animate({"left": -($("#slide_" + index).position().left)}, 600);


//    $("#small-clouds").animate({"left": -100 * index}, 700);
//    $("#big-clouds").animate({"left": -350 * index}, 700);

    //setting the header..
    currentSlide = index;

}

function showNotes(obj){
    var serviceName= $(obj).parent().attr('data-service');
    var target = $(obj);
    $.ajax({
               url:'add-note.jsp',
               data:{"serviceName":serviceName},
               success:function(data){
                   $( "#dialog" ).html(data);
                   $( "#dialog" ).attr('title','');
                   $('#dialog').dialog({title:'', width: 300 }).dialog('widget').position({ my: 'left top', at: 'right', of:target });

               }
           });

}
function addNote(obj,serviceName){
    var currentTime = new Date();

    var month = currentTime.getMonth() + 1;
    var day = currentTime.getDate();
    var year = currentTime.getFullYear();
    var hours = currentTime.getHours();
    var minutes = currentTime.getMinutes();

    if (minutes < 10){
        minutes = "0" + minutes
    }

    var dateString = month + "/" + day + "/" + year + " " + hours + ":" + minutes + " ";
    if(hours > 11){
        dateString += "PM";
    } else {
        dateString += "AM";
    }

    // Got the current time to a string now (dateString) now have to append it to the note.

    var note = $('textarea',$(obj).parent()).val();

    var noteAddHelper = function(target){
        return function(data){
            $( "#dialog" ).html(data);
            $( "#dialog" ).attr('title','');
            $('#dialog').dialog({ title:'',width: 300 }).dialog('widget').position({ my: 'left top', at: 'right', of:target });
        }
    };
    $.ajax({
               url:'add-note.jsp',
               data:{'note':note,'serviceName':serviceName},
               success:noteAddHelper()
           });

}
function showLoginDialog(obj){
    var target = $(obj);
    $( "#dialog" ).html(
        $('#dialog-login-data').html()
    );
    $('#dialog').dialog({modal:true, title:'Login',width: 300 }).dialog('widget').position({ my: 'left bottom', at: 'left', of:target });

}

function changePassword(obj){
    var target = $(obj);
    $( "#dialog" ).html(
        $('#dialog-pw-change-data').html()
    );
    $('#dialog').dialog({modal:true, title:'Change Password',width: 400 }).dialog('widget').position({ my: 'left bottom', at: 'left', of:target });

}
function validatePChangeForm(form){
    if($('.n_password',form).val()!=$('.nr_password',form).val()){
        $('.error-box',form).html('New Password and Retype New Password fields do not match.').show();
        return false;
    }else if($('.n_password',form).val()==""||$('.nr_password',form).val()==""){
        $('.error-box',form).html('Password can\'t be empty.').show();
        return false;
    }else{
        return true;
    }
}

function showHistoryNotes(obj){
    var target = $(obj);
    $( "#dialog" ).html($('.history-note-data',$(obj).parent()).html());
    $( "#dialog" ).attr('title','');
    $('#dialog').dialog({title:'', width: 300 }).dialog('widget').position({ my: 'left top', at: 'right', of:target });
}