local limits = ARGV
local len = #ARGV
local ret = false
for i=1,len,3 do
    local fullkey = KEYS[1] .. ':' .. limits[i]
    local bucket = redis.call('GET', fullkey)
    if ( (bucket ~= false) and ( tonumber(bucket) > tonumber(limits[i])) ) then
        ret = i
    else
        local count = redis.call('INCR', fullkey)
        if tonumber(count) > tonumber(limits[i]) then
            redis.call('PEXPIRE', fullkey, tonumber(limits[i+2]))
            ret = i
        else
            if count == 1 then
              redis.call('PEXPIRE', fullkey, tonumber(ARGV[i+1]))
            end
        end
    end
end
return ret

