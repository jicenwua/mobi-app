"use strict";exports.withClickLock=function(t){let i=!1;return async function(...n){if(!i){i=!0;try{return await t.apply(this,n)}finally{i=!1}}}};
