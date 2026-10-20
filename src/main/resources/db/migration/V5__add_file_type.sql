ALTER TABLE message 
DROP CONSTRAINT message_message_type_check;

ALTER TABLE message 
ADD CONSTRAINT message_message_type_check 
CHECK (message_type::text = ANY (ARRAY['TEXT'::text, 'IMAGE'::text, 'REACTION'::text, 'VIDEO'::text,'STICKER'::text, 'SYSTEM'::text, 'FILE'::text]));
