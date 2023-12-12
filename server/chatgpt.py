from openai import OpenAI
import json
import os


client = OpenAI(
    #api_key="",
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

    def ask(self, target="[[-5,0,-5,blue],[-5,1,-5,blue],[-5,2,-5,yellow]]",message=None):
        if message:
            self.add_message("user", message)
        setup_message = {   
            "role": "system",
            #"content": "You are an agent in a voxel world, where the most northernly point is 0,0,-5; the most westerly point -5,0,0; the most eastern point is 5,0,0; the most southern 0,0,5 and the y-axis is up and down, with y=0 being the minimum. Describe the coordinates of the blocks and their colours in a nested list JSON format [[x,y,z,color], ...] according to the user description. Give only the JSON in your response, no additional dialog.",
            #"content": "You are an agent in a voxel world, where the most northernly point is 0,0,-5; the most westerly point -5,0,0; the most eastern point is 5,0,0; the most southern 0,0,5 and the y-axis is up and down, with y=0 being the minimum. Describe the coordinates of the blocks and their colours in a nested list JSON format [[x,y,z,color], ...] according to the user description. Give all possible interpretations in a JSON format in your response with a confidence score, but no additional dialog.",
            "content": f"You are an agent in a voxel world, where the most northernly point is 0,0,-5; the most westerly point -5,0,0; the most eastern point is 5,0,0; the most southern 0,0,5 and the y-axis is up and down, with y=0 being the minimum. Your task is to give instructions to a human to place blocks to achieve the target world state: {target} where the target world instructions are in the format [[x,y,z,color],...].  Give easy to interpret instructions, do not directly mention the coordinates. The builder will respond with the coordinates of the blocks they have placed in the same format.  Don't ask for coordinates, they will always be given. Avoid long instructions with multiple steps.",
        }
        messages = [setup_message] + self.messages
        print("sending", messages)
        chat_completion = client.chat.completions.create(
            messages=messages,
            #model="gpt-3.5-turbo",
            model=self.model,
        )
        response = chat_completion.choices[0].message.content
        self.add_message("assistant", response)
        return response

if __name__ == "__main__":
    mg = MinecraftGPT()
    print(mg.ask("[[0,0,0,blue]]"))
