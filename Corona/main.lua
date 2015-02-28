local widget = require "widget"
local library = require "plugin.library"

local g_bTestMode = true
function print_r(arr, indentLevel)
	if(not g_bTestMode) then return end
    local str = ""
    local indentStr = "#"

    if(indentLevel == nil) then
        print(print_r(arr, 0))
        return
    end

    for i = 0, indentLevel do
        indentStr = indentStr.."\t"
    end

    for index,value in pairs(arr) do
    	if type(value) == "table" then
            str = str..indentStr..index..": \n"..print_r(value, (indentLevel + 1))
        elseif( type(value) ~= "userdata") then
            str = str..indentStr..index..": "..value.."\n"
        end
    end
    return str
end

contact_address = library.getAllContacts()
print_r(contact_address)

local function onRowRender(event)
    local row = event.row
    local rowHeight = row.contentHeight

    local labelParameters = {
        parent = row,
        x = 20,
        y = 30, 
        text = row.params.tIdx,
        width = 40,
        height = 40,
        font = native.systemFont,
        fontSize = 14, 
        align = "center"
    }
    print( "idx = " .. row.params.tIdx )
    local rowNo = display.newText(labelParameters)
    rowNo:setFillColor( 0 )

    labelParameters.text = contact_address[row.params.mIdx].name
    labelParameters.x = 90
    labelParameters.width = 100
    local rowName = display.newText(labelParameters)
    rowName:setFillColor( 0 )

    labelParameters.text = contact_address[row.params.mIdx].phones[row.params.sIdx].phonenumber
    labelParameters.x = 230
    labelParameters.width = 180
    local rowNumber = display.newText(labelParameters)

    rowNumber:setFillColor( 0 )
end

local bgWhite = display.newRect( display.contentWidth/2, display.contentHeight/2, display.contentWidth, display.contentHeight )

local options = {x=display.contentWidth/2, y=display.contentHeight/2+20, width=display.contentWidth, height=display.contentHeight-40, onRowRender=onRowRender}
local contactsTable = widget.newTableView(options)

local tCount = 0
for i,v in pairs(contact_address) do
    print("get address , idx=" .. i)
    for j, v1 in pairs(v.phones) do
        print("get address , sub idx=" .. j)
        tCount = tCount + 1
        contactsTable:insertRow( {rowHeight=40, params={mIdx=i, sIdx = j, tIdx = tCount, rowColor={default={0,0.8,0.8,0.8}}}} )
    end
end




