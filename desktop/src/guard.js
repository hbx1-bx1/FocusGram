(function() {
var A=['/direct','/call','/accounts','/challenge','/two_factor','/re_login'];
var I='https://www.instagram.com/direct/inbox/';
var oC=window.close;window.close=function(){if(window.location.pathname.indexOf('/call')===0){window.location.replace(I)}else if(oC){oC()}}
function c(p){for(var i=0;i<A.length;i++){var a=A[i];if(p===a||p.indexOf(a+'/')===0)return true}return false}
function r(){var p=window.location.pathname;if(!c(p)){window.location.replace(I)}}
(function(h){var ps=h.pushState;h.pushState=function(){ps.apply(h,arguments);setTimeout(r,10)};var rs=h.replaceState;h.replaceState=function(){rs.apply(h,arguments);setTimeout(r,10)}})(window.history)
window.addEventListener('popstate',r)
document.addEventListener('click',function(e){
var t=e.target.closest('a[href]');if(t){var u=t.getAttribute('href');if(u){try{var p=new URL(u,window.location.origin).pathname;if(!c(p)){e.preventDefault();e.stopImmediatePropagation();window.location.replace(I);return}}catch(ex){}}}
var n=e.target.closest('[aria-label*="reel"i],[aria-label*="story"i],[aria-label*="post"i],[aria-label*="profile"i]');if(n){e.preventDefault();e.stopImmediatePropagation();window.location.replace(I)}
},true)
function stopLinkedMedia(){document.querySelectorAll('a[href*="/reels"],a[href*="/reel"],a[href*="/stories"],a[href*="/story"],a[href*="/p/"]').forEach(function(e){e.querySelectorAll('video,audio').forEach(function(m){m.pause();m.muted=true;m.removeAttribute('src');m.load();m.remove()})})}
var S='a[href*="/explore"],a[href*="/reels"],a[href*="/reel"],a[href*="/stories"],a[href*="/story"],a[href*="/p/"],a[href*="/tv"],a[href*="/shop"],a[href*="/create"],a[href*="/accounts/edit"],a[href*="/settings"]';
function d(){document.querySelectorAll(S).forEach(function(e){e.removeAttribute('href');e.setAttribute('aria-disabled','true');e.style.pointerEvents='none'});if(window.location.pathname.indexOf('/call')!==0){stopLinkedMedia()}}
var s=document.createElement('style');s.id='fg-css';s.textContent='div[role="complementary"],footer{display:none!important}html,body{width:100%!important;max-width:100%!important;overflow-x:hidden!important}';document.head.appendChild(s)
d();var o=new MutationObserver(d);o.observe(document.body,{childList:true,subtree:true})
setInterval(function(){d();r()},250)
})();
