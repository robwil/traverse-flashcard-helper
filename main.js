// ==UserScript==
// @name         Traverse Flashcard Helper
// @namespace    https://robwilliams.me/
// @version      1.9
// @description  Adds actor, set, and prop descriptions to Movie Review flashcard notes.
// @author       Rob Williams (and Gemini Pro 2.5)
// @match        *://traverse.link/*
// @grant        GM_getValue
// @grant        GM_setValue
// ==/UserScript==

(function() {
    'use strict';

    const flashcardSelector = '.ProseMirror';

    // --- Helper to determine tone location from the Set data (No changes) ---
    function getToneLocation(pinyin, setData) {
        if (!pinyin || !setData) return '';
        if (/[āēīōūĀĒĪŌŪ]/.test(pinyin)) return "Front";
        if (/[áéíóúǘÁÉÍÓÚǗ]/.test(pinyin)) return setData.tone2 || "Kitchen/cafeteria";
        if (/[ǎěǐǒǔǚǍĚǏǑǓǙ]/.test(pinyin)) return setData.tone3 || "Bedroom/office/living room";
        if (/[àèìòùǜÀÈÌÒÙǛ]/.test(pinyin)) return setData.tone4 || "Bathroom/backyard";
        return "Roof";
    }

    // --- Helper for simple key-value pairs (Actor, Props) (No changes) ---
    async function getOrSet_SimpleValue(key) {
        if (!key) return '';
        let storedValue = await GM_getValue(key, null);
        if (storedValue !== null) return storedValue;
        let userValue = prompt(`No value found for key: "${key}"\n\nPlease enter the value to use:`);
        if (userValue) {
            await GM_setValue(key, userValue);
            return userValue;
        }
        return key;
    }

    // --- Helper for the complex "Set" object (No changes) ---
    async function getOrSet_SetData(key) {
        if (!key) return { name: '', tone2: '', tone3: '', tone4: '' };
        let storedData = await GM_getValue(key, null);
        if (storedData && typeof storedData === 'object') return storedData;
        alert(`New "Set" detected for key: "${key}"\n\nYou will be asked for its name and tone locations.`);
        const setName = prompt(`Enter the name for this Set:`);
        if (!setName) return { name: key, tone2: '', tone3: '', tone4: '' };
        const tone2Location = prompt(`Enter the 2nd tone location (e.g., Kitchen):`, "Kitchen/cafeteria");
        const tone3Location = prompt(`Enter the 3rd tone location (e.g., Bedroom):`, "Bedroom/office/living room");
        const tone4Location = prompt(`Enter the 4th tone location (e.g., Backyard):`, "Bathroom/backyard");
        const newSetData = {
            name: setName,
            tone2: tone2Location,
            tone3: tone3Location,
            tone4: tone4Location
        };
        await GM_setValue(key, newSetData);
        return newSetData;
    }

    // --- Permanent container setup (No changes) ---
    const permanentContainer = document.createElement('div');
    permanentContainer.id = 'my-stable-button-container';
    permanentContainer.style.cssText = `position: fixed; top: 20px; right: 100px; z-index: 9999;`;
    document.body.appendChild(permanentContainer);

    // --- Main button creation function ---
    function createOrUpdateButton() {
        if (document.getElementById('my-custom-flashcard-button')) return;
        const myButton = document.createElement('button');
        myButton.id = 'my-custom-flashcard-button';
        myButton.textContent = '📝 Add Memory Palace Notes';
        myButton.style.cssText = 'padding: 8px 12px; font-size: 14px; cursor: pointer; border-radius: 5px; border: 1px solid #ccc; background-color: #f0f0f0; box-shadow: 0 2px 5px rgba(0,0,0,0.2);';

        myButton.onclick = async function() {
            const flashcardContent = document.querySelector(flashcardSelector);
            if (!flashcardContent) {
                alert('Could not find flashcard content.');
                return;
            }

            // ===================================================================
            // THIS DATA EXTRACTION LOOP IS NOW MORE ROBUST
            // ===================================================================
            const paragraphs = flashcardContent.querySelectorAll('p');
            let rawActorKey = '', rawSetKey = '', pinyin = '', rawProps = [];
            let isPropsSection = false;

            for (let i = 0; i < paragraphs.length; i++) {
                const p = paragraphs[i];
                const strongTag = p.querySelector('strong');
                const strongText = strongTag ? strongTag.textContent.trim() : '';

                if (strongText.startsWith('Pinyin')) {
                    // Search forward from the Pinyin label for the next non-empty line
                    for (let j = i + 1; j < paragraphs.length; j++) {
                        const nextP = paragraphs[j];
                        // Stop if we hit another section header
                        if (nextP.querySelector('strong')) {
                            break;
                        }
                        const pinyinText = nextP.textContent.trim();
                        // If we find a line with text, it's the Pinyin
                        if (pinyinText) {
                            pinyin = pinyinText;
                            break; // Exit inner loop once found
                        }
                    }
                } else if (strongText.startsWith('Actor:')) {
                    isPropsSection = false;
                    rawActorKey = p.querySelector('a')?.textContent || '';
                } else if (strongText.startsWith('Set:')) {
                    isPropsSection = false;
                    rawSetKey = p.querySelector('a')?.textContent || '';
                } else if (strongText.startsWith('Prop(s):')) {
                    isPropsSection = true;
                } else if (strongTag) {
                    isPropsSection = false;
                }

                if (isPropsSection) {
                    p.querySelectorAll('a').forEach(link => rawProps.push(link.textContent));
                }
            }
            // ===================================================================
            // END OF UPDATED SECTION
            // ===================================================================

            const finalActor = await getOrSet_SimpleValue(rawActorKey);
            const setData = await getOrSet_SetData(rawSetKey);
            const toneLocation = getToneLocation(pinyin, setData);

            const propKeys = rawProps.map(prop => prop.replace(/（PROP）/g, '').trim());
            const propPromises = propKeys.map(key => getOrSet_SimpleValue(key));
            const finalProps = await Promise.all(propPromises);

            const finalSetWithTone = `${setData.name} ${toneLocation}`.trim();

            const resultText = `Actor: ${finalActor}\nSet: ${finalSetWithTone}\nProps: ${finalProps.join(', ')}\n`;
            const notesEditor = document.querySelector('div[id$="-field-NOTES"] .ProseMirror[contenteditable="true"]');

            if (notesEditor) {
                notesEditor.focus();
                document.execCommand('insertText', false, resultText);
            } else {
                alert('Notes field not found!');
            }
        };
        permanentContainer.appendChild(myButton);
    }

    // --- MutationObserver (No changes) ---
    const observerCallback = function(mutationsList, observer) {
        const flashcardNode = document.querySelector(flashcardSelector);
        const buttonNode = document.getElementById('my-custom-flashcard-button');
        if (flashcardNode && !buttonNode) {
            createOrUpdateButton();
        } else if (!flashcardNode && buttonNode) {
            buttonNode.remove();
        }
    };
    const observer = new MutationObserver(observerCallback);
    observer.observe(document.body, { childList: true, subtree: true });

})();
