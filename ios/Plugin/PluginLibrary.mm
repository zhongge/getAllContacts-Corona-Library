//
//  PluginLibrary.mm
//  TemplateApp
//
//  Copyright (c) 2012 __MyCompanyName__. All rights reserved.
//

#import "PluginLibrary.h"

#include "CoronaRuntime.h"

#import <UIKit/UIKit.h>
#import "AddressBook/ABAddressBook.h"
#import "AddressBook/ABPerson.h"
#import "AddressBook/ABMultiValue.h"
// ----------------------------------------------------------------------------

class PluginLibrary
{
	public:
		typedef PluginLibrary Self;

	public:
		static const char kName[];
		static const char kEvent[];

	protected:
		PluginLibrary();

	public:
		bool Initialize( CoronaLuaRef listener );

	public:
		CoronaLuaRef GetListener() const { return fListener; }

	public:
		static int Open( lua_State *L );

	protected:
		static int Finalizer( lua_State *L );

	public:
		static Self *ToLibrary( lua_State *L );

	public:
		static int getAllContacts( lua_State *L );
	private:
		CoronaLuaRef fListener;
};

// ----------------------------------------------------------------------------

// This corresponds to the name of the library, e.g. [Lua] require "plugin.library"
const char PluginLibrary::kName[] = "plugin.library";

// This corresponds to the event name, e.g. [Lua] event.name
const char PluginLibrary::kEvent[] = "pluginlibraryevent";

PluginLibrary::PluginLibrary()
:	fListener( NULL )
{
}

bool
PluginLibrary::Initialize( CoronaLuaRef listener )
{
	// Can only initialize listener once
	bool result = ( NULL == fListener );

	if ( result )
	{
		fListener = listener;
	}

	return result;
}

int
PluginLibrary::Open( lua_State *L )
{
	// Register __gc callback
	const char kMetatableName[] = __FILE__; // Globally unique string to prevent collision
	CoronaLuaInitializeGCMetatable( L, kMetatableName, Finalizer );

	// Functions in library
	const luaL_Reg kVTable[] =
	{
		{ "getAllContacts", getAllContacts },
		{ NULL, NULL }
	};

	// Set library as upvalue for each library function
	Self *library = new Self;
	CoronaLuaPushUserdata( L, library, kMetatableName );

	luaL_openlib( L, kName, kVTable, 1 ); // leave "library" on top of stack

	return 1;
}

int
PluginLibrary::Finalizer( lua_State *L )
{
	Self *library = (Self *)CoronaLuaToUserdata( L, 1 );

	CoronaLuaDeleteRef( L, library->GetListener() );

	delete library;

	return 0;
}

PluginLibrary *
PluginLibrary::ToLibrary( lua_State *L )
{
	// library is pushed as part of the closure
	Self *library = (Self *)CoronaLuaToUserdata( L, lua_upvalueindex( 1 ) );
	return library;
}

// [Lua] library.init( listener )
int
PluginLibrary::getAllContacts( lua_State *L )
{
    CFErrorRef *error = nil;
    ABAddressBookRef addressBook = ABAddressBookCreateWithOptions(NULL, error);
    
    __block BOOL accessGranted = NO;
    if (ABAddressBookRequestAccessWithCompletion != NULL) { // we're on iOS 6
        dispatch_semaphore_t sema = dispatch_semaphore_create(0);
        ABAddressBookRequestAccessWithCompletion(addressBook, ^(bool granted, CFErrorRef error) {
            accessGranted = granted;
            dispatch_semaphore_signal(sema);
        });
        dispatch_semaphore_wait(sema, DISPATCH_TIME_FOREVER);
        
    }
    else { // we're on iOS 5 or older
        accessGranted = YES;
    }
    
    if (accessGranted) {
        CFArrayRef allPeople = ABAddressBookCopyArrayOfAllPeople( addressBook );
        CFIndex nPeople = ABAddressBookGetPersonCount( addressBook );
        lua_createtable(L, nPeople, 0);
        for ( int i = 0; i < nPeople; i++ )
        {
            ABRecordRef ref = CFArrayGetValueAtIndex( allPeople, i );
            NSString *firstName = (__bridge NSString *)(ABRecordCopyValue(ref, kABPersonFirstNameProperty));
            NSString *lastName = (__bridge NSString *)(ABRecordCopyValue(ref, kABPersonLastNameProperty));
            NSString *fullName = [NSString stringWithFormat:@"%@ %@", firstName, lastName];
            
            ABMultiValueRef phones = ABRecordCopyValue(ref, kABPersonPhoneProperty);
            int phoneCount = ABMultiValueGetCount(phones);
            
            lua_pushinteger(L, i+1);
            lua_createtable(L, 2, 0);
            lua_pushstring(L, [fullName UTF8String]);
            lua_setfield(L, -2, "name");
            
            lua_pushstring(L, "phones");
            lua_createtable(L, phoneCount, 0);
            
            for(CFIndex j = 0; j < phoneCount; j++)
            {
                CFStringRef phoneNumberRef = (CFStringRef)ABMultiValueCopyValueAtIndex(phones, j);
                CFStringRef locLabel = ABMultiValueCopyLabelAtIndex(phones, j);
                NSString *phoneLabel =(NSString*) ABAddressBookCopyLocalizedLabel(locLabel);
                //CFRelease(phones);
                NSString *phoneNumber = (NSString *)phoneNumberRef;
                lua_pushinteger(L, j+1);
                lua_createtable(L, 0, 2);
                lua_pushstring(L, [((NSString*)phoneLabel) UTF8String]);
                lua_setfield(L, -2, "label");
                
                lua_pushstring(L, [phoneNumber UTF8String]);
                lua_setfield(L, -2, "phonenumber");
                
                lua_settable(L, -3);
                
                CFRelease(phoneNumberRef);
                CFRelease(locLabel);
                NSLog(@"  - %@ (%@)", phoneNumber, phoneLabel);
                [phoneNumber release];
            }
            
            lua_settable(L, -3);
            lua_settable(L, -3);
        }
    }
	return 1;
}
// ----------------------------------------------------------------------------

CORONA_EXPORT int luaopen_plugin_library( lua_State *L )
{
	return PluginLibrary::Open( L );
}
