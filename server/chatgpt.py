from openai import OpenAI
import json
import os


client = OpenAI(
#    api_key=os.environ.get("OPENAI_API_KEY"),
)

class MinecraftGPT:
    def __init__(self, model="gpt-4"):
        self.messages = []
        self.model = model

    def add_message(self, role, message):
        assert role in ["user", "assistant"]
        self.messages.append({"role": role, "content": message})

    def encode(self):
        return json.dumps(self.messages)

    @staticmethod
    def fromEncoded(encoded):
        x = MinecraftGPT()
        x.messages = json.loads(encoded)
        return x

    def ask(self, message):
        self.add_message("user", message)
        setup_message = {   
            "role": "system",
            #"content": "You are an agent in a voxel world, where the most northernly point is 0,0,-5; the most westerly point -5,0,0; the most eastern point is 5,0,0; the most southern 0,0,5 and the y-axis is up and down, with y=0 being the minimum. Describe the coordinates of the blocks and their colours in a nested list JSON format [[x,y,z,color], ...] according to the user description. Give only the JSON in your response, no additional dialog.",
            #"content": "You are an agent in a voxel world, where the most northernly point is 0,0,-5; the most westerly point -5,0,0; the most eastern point is 5,0,0; the most southern 0,0,5 and the y-axis is up and down, with y=0 being the minimum. Describe the coordinates of the blocks and their colours in a nested list JSON format [[x,y,z,color], ...] according to the user description. Give all possible interpretations in a JSON format in your response with a confidence score, but no additional dialog.",
            "content": "You are an agent in a voxel world, where the most northernly point is 0,0,-5; the most westerly point -5,0,0; the most eastern point is 5,0,0; the most southern 0,0,5 and the y-axis is up and down, with y=0 being the minimum. Describe the coordinates of the blocks their colours (must be one of: blue, yellow, green, orange, purple, red) and whether the action is to add or remove them, your confidence in your interpretation of the instruction and optionally a question if the instruction is potentially unclear, in the JSON format: {\"add\": [[x,y,z,color], ...], \"remove\": [[x,y,z,color], ...], \"confidence\": 0.0, \"question\": \"...\"}.  Give the JSON only, no additional dialog.",
        }
        messages = [setup_message] + self.messages
        print("sending", messages)
        chat_completion = client.chat.completions.create(
            messages=messages,
            #model="gpt-3.5-turbo",
            model=self.model,
        )
        response = chat_completion.choices[0].message.content
        try:
            parsed = json.loads(response)
            self.add_message("assistant", response)
            return parsed
        except:
            print(f"error parsing response: {response}")
            self.add_message("assistant", "")
            return []

if __name__ == "__main__":
    mg = MinecraftGPT()
    print(mg.ask("make a tower of 3 blocks in a diagonal"))
